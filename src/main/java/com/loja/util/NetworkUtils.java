package com.loja.util;

import com.loja.model.TerminalSessao;
import com.loja.repository.ConnectionFactory;

import java.net.*;
import java.util.*;

public class NetworkUtils {

    /**
     * Verifica se o endereço IPv4 pertence à faixa CGNAT do Tailscale (100.64.0.0/10: de 100.64.0.0 até 100.127.255.255)
     */
    public static boolean isTailscale(InetAddress addr) {
        if (addr == null) return false;
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return first == 100 && (second >= 64 && second <= 127);
        }
        return false;
    }

    /**
     * Verifica se a interface de rede é virtual, VPN, túnel ou Tailscale
     */
    public static boolean isTailscaleOrVirtualInterface(NetworkInterface ni) {
        if (ni == null) return false;
        String name = (ni.getName() + " " + ni.getDisplayName()).toLowerCase();
        return name.contains("tailscale") ||
               name.contains("wireguard") ||
               name.contains("wg") ||
               name.contains("docker") ||
               name.contains("wsl") ||
               name.contains("default switch") ||
               name.contains("virtualbox") ||
               name.contains("vbox") ||
               name.contains("vmware") ||
               name.contains("bluetooth") ||
               name.contains("loopback") ||
               name.contains("tap") ||
               name.contains("tun") ||
               name.contains("pseudo") ||
               name.contains("teredo") ||
               name.contains("isatap");
    }

    /**
     * Verifica se o endereço IPv4 deve ser ignorado para comunicação em rede local
     */
    public static boolean isIgnoredAddress(InetAddress addr) {
        if (addr == null) return true;
        if (!(addr instanceof Inet4Address)) return true;
        if (addr.isLoopbackAddress()) return true;
        if (addr.isLinkLocalAddress()) return true; // 169.254.x.x (APIPA)
        if (addr.isAnyLocalAddress()) return true;  // 0.0.0.0
        return isTailscale(addr);
    }

    /**
     * Obtém o IP da máquina local conectado ao banco de dados MySQL.
     * Prioriza a interface exata usada para rotear até o IP do banco configurado.
     */
    public static String obterIpConectadoAoBanco(String dbHost, String dbPort) {
        // 1. Se o host do banco for um IP ou nome remoto (diferente de localhost / 127.0.0.1)
        if (dbHost != null && !dbHost.trim().isEmpty() &&
                !dbHost.equalsIgnoreCase("localhost") &&
                !dbHost.equals("127.0.0.1") &&
                !dbHost.equals("::1")) {

            try (DatagramSocket socket = new DatagramSocket()) {
                int port = 3306;
                if (dbPort != null && !dbPort.trim().isEmpty()) {
                    try { port = Integer.parseInt(dbPort.trim()); } catch (Exception ignored) {}
                }
                socket.connect(InetAddress.getByName(dbHost.trim()), port);
                InetAddress localAddr = socket.getLocalAddress();
                if (localAddr instanceof Inet4Address && !isIgnoredAddress(localAddr)) {
                    return localAddr.getHostAddress();
                }
            } catch (Exception ignored) {}
        }

        // 2. Se for localhost / banco local ou se a consulta de rota falhar, varre as interfaces físicas locais
        String ipFisico = obterMelhorIpRedeLocal(false);
        if (ipFisico != null) {
            return ipFisico;
        }

        // 3. Fallback permitindo interfaces virtuais (exceto Tailscale)
        String ipFallback = obterMelhorIpRedeLocal(true);
        if (ipFallback != null) {
            return ipFallback;
        }

        // 4. Último fallback via InetAddress.getLocalHost(), rejeitando Tailscale
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost instanceof Inet4Address && !isTailscale(localHost) && !localHost.isLoopbackAddress()) {
                return localHost.getHostAddress();
            }
        } catch (Exception ignored) {}

        return "127.0.0.1";
    }

    private static String obterMelhorIpRedeLocal(boolean aceitarVirtuaisNaoTailscale) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<String> prioridade192 = new ArrayList<>();
            List<String> prioridade10 = new ArrayList<>();
            List<String> prioridade172 = new ArrayList<>();
            List<String> outrosIps = new ArrayList<>();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                if (!aceitarVirtuaisNaoTailscale && isTailscaleOrVirtualInterface(ni)) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (isIgnoredAddress(addr)) continue;

                    String ip = addr.getHostAddress();
                    if (ip.startsWith("192.168.")) {
                        prioridade192.add(ip);
                    } else if (ip.startsWith("10.")) {
                        prioridade10.add(ip);
                    } else if (ip.startsWith("172.")) {
                        // Verifica se está na faixa de IP privado 172.16 a 172.31
                        byte[] b = addr.getAddress();
                        int octet2 = b[1] & 0xFF;
                        if (octet2 >= 16 && octet2 <= 31) {
                            prioridade172.add(ip);
                        } else {
                            outrosIps.add(ip);
                        }
                    } else {
                        outrosIps.add(ip);
                    }
                }
            }

            if (!prioridade192.isEmpty()) return prioridade192.get(0);
            if (!prioridade10.isEmpty()) return prioridade10.get(0);
            if (!prioridade172.isEmpty()) return prioridade172.get(0);
            if (!outrosIps.isEmpty()) return outrosIps.get(0);

        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Retorna o IP da Máquina Matriz (Servidor MySQL) configurado na conexão de banco de dados.
     * Se for localhost / 127.0.0.1, resolve para o IP real desta máquina na rede local (excluindo Tailscale).
     */
    public static String obterIpMatriz(String hostConfigurado) {
        if (hostConfigurado != null && !hostConfigurado.trim().isEmpty() &&
                !hostConfigurado.equalsIgnoreCase("localhost") &&
                !hostConfigurado.equals("127.0.0.1") &&
                !hostConfigurado.equals("::1")) {
            return hostConfigurado.trim();
        }
        return obterIpConectadoAoBanco("localhost", "3306");
    }

    /**
     * Verifica se esta máquina atual é a Máquina Matriz (Servidor)
     */
    public static boolean isEstaMaquinaMatriz() {
        String host = ConnectionFactory.getHost();
        if (host == null || host.trim().isEmpty() ||
                host.equalsIgnoreCase("localhost") ||
                host.equals("127.0.0.1") ||
                host.equals("::1")) {
            return true;
        }

        String ipMatriz = host.trim();

        // Verifica se o IP do host configurado pertence a alguma das placas de rede desta máquina
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().equalsIgnoreCase(ipMatriz)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Verifica se uma sessão de terminal representa a Máquina Matriz (Servidor)
     */
    public static boolean isTerminalMatriz(TerminalSessao s, String proprioSessaoId) {
        if (s == null || s.getIp() == null) return false;
        String ipMatriz = obterIpMatriz(ConnectionFactory.getHost());
        if (s.getIp().trim().equalsIgnoreCase(ipMatriz.trim())) {
            return true;
        }
        if (isEstaMaquinaMatriz() && proprioSessaoId != null && proprioSessaoId.equals(s.getId())) {
            return true;
        }
        return false;
    }
}
