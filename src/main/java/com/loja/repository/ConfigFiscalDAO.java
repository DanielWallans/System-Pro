package com.loja.repository;

import com.loja.model.ConfigFiscal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfigFiscalDAO {

    public ConfigFiscal obterConfiguracao() {
        String sql = "SELECT * FROM config_fiscal WHERE id = 1";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                ConfigFiscal c = new ConfigFiscal();
                c.setId(rs.getInt("id"));
                c.setCnpj(rs.getString("cnpj"));
                c.setInscricaoEstadual(rs.getString("inscricao_estadual"));
                c.setRazaoSocial(rs.getString("razao_social"));
                c.setNomeFantasia(rs.getString("nome_fantasia"));
                c.setRegimeTributario(rs.getInt("regime_tributario"));
                c.setAmbiente(rs.getInt("ambiente"));
                c.setCscToken(rs.getString("csc_token"));
                c.setCscId(rs.getString("csc_id"));
                c.setSerieNfce(rs.getInt("serie_nfce"));
                c.setUltimoNumeroNfce(rs.getInt("ultimo_numero_nfce"));
                c.setSerieNfe(rs.getInt("serie_nfe"));
                c.setUltimoNumeroNfe(rs.getInt("ultimo_numero_nfe"));
                c.setCertificadoCaminho(rs.getString("certificado_caminho"));
                c.setCertificadoSenha(rs.getString("certificado_senha"));
                c.setLogradouro(rs.getString("logradouro"));
                c.setNumero(rs.getString("numero"));
                c.setComplemento(rs.getString("complemento"));
                c.setBairro(rs.getString("bairro"));
                c.setMunicipio(rs.getString("municipio"));
                c.setCodigoIbge(rs.getString("codigo_ibge"));
                c.setUf(rs.getString("uf"));
                c.setCep(rs.getString("cep"));
                c.setTelefone(rs.getString("telefone"));
                return c;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter configurações fiscais: " + e.getMessage());
        }

        // Retorna configuração padrão se não encontrada
        return new ConfigFiscal();
    }

    public boolean salvar(ConfigFiscal config) {
        String sql = "INSERT INTO config_fiscal (id, cnpj, inscricao_estadual, razao_social, nome_fantasia, " +
                     "regime_tributario, ambiente, csc_token, csc_id, serie_nfce, ultimo_numero_nfce, " +
                     "serie_nfe, ultimo_numero_nfe, certificado_caminho, certificado_senha, logradouro, " +
                     "numero, complemento, bairro, municipio, codigo_ibge, uf, cep, telefone) " +
                     "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE cnpj = ?, inscricao_estadual = ?, razao_social = ?, nome_fantasia = ?, " +
                     "regime_tributario = ?, ambiente = ?, csc_token = ?, csc_id = ?, serie_nfce = ?, ultimo_numero_nfce = ?, " +
                     "serie_nfe = ?, ultimo_numero_nfe = ?, certificado_caminho = ?, certificado_senha = ?, logradouro = ?, " +
                     "numero = ?, complemento = ?, bairro = ?, municipio = ?, codigo_ibge = ?, uf = ?, cep = ?, telefone = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // INSERT (1 a 23)
            stmt.setString(1, config.getCnpj());
            stmt.setString(2, config.getInscricaoEstadual());
            stmt.setString(3, config.getRazaoSocial());
            stmt.setString(4, config.getNomeFantasia());
            stmt.setInt(5, config.getRegimeTributario());
            stmt.setInt(6, config.getAmbiente());
            stmt.setString(7, config.getCscToken());
            stmt.setString(8, config.getCscId());
            stmt.setInt(9, config.getSerieNfce());
            stmt.setInt(10, config.getUltimoNumeroNfce());
            stmt.setInt(11, config.getSerieNfe());
            stmt.setInt(12, config.getUltimoNumeroNfe());
            stmt.setString(13, config.getCertificadoCaminho());
            stmt.setString(14, config.getCertificadoSenha());
            stmt.setString(15, config.getLogradouro());
            stmt.setString(16, config.getNumero());
            stmt.setString(17, config.getComplemento());
            stmt.setString(18, config.getBairro());
            stmt.setString(19, config.getMunicipio());
            stmt.setString(20, config.getCodigoIbge());
            stmt.setString(21, config.getUf());
            stmt.setString(22, config.getCep());
            stmt.setString(23, config.getTelefone());

            // UPDATE (24 a 46)
            stmt.setString(24, config.getCnpj());
            stmt.setString(25, config.getInscricaoEstadual());
            stmt.setString(26, config.getRazaoSocial());
            stmt.setString(27, config.getNomeFantasia());
            stmt.setInt(28, config.getRegimeTributario());
            stmt.setInt(29, config.getAmbiente());
            stmt.setString(30, config.getCscToken());
            stmt.setString(31, config.getCscId());
            stmt.setInt(32, config.getSerieNfce());
            stmt.setInt(33, config.getUltimoNumeroNfce());
            stmt.setInt(34, config.getSerieNfe());
            stmt.setInt(35, config.getUltimoNumeroNfe());
            stmt.setString(36, config.getCertificadoCaminho());
            stmt.setString(37, config.getCertificadoSenha());
            stmt.setString(38, config.getLogradouro());
            stmt.setString(39, config.getNumero());
            stmt.setString(40, config.getComplemento());
            stmt.setString(41, config.getBairro());
            stmt.setString(42, config.getMunicipio());
            stmt.setString(43, config.getCodigoIbge());
            stmt.setString(44, config.getUf());
            stmt.setString(45, config.getCep());
            stmt.setString(46, config.getTelefone());

            stmt.executeUpdate();
            System.out.println("[DB] Configurações fiscais da loja salvas com sucesso.");
            return true;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao salvar configurações fiscais: " + e.getMessage());
            return false;
        }
    }

    public int proximoNumeroNfce() {
        String sql = "UPDATE config_fiscal SET ultimo_numero_nfce = ultimo_numero_nfce + 1 WHERE id = 1";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            ConfigFiscal c = obterConfiguracao();
            return c.getUltimoNumeroNfce();
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao incrementar número NFC-e: " + e.getMessage());
            return 1;
        }
    }

    public int proximoNumeroNfe() {
        String sql = "UPDATE config_fiscal SET ultimo_numero_nfe = ultimo_numero_nfe + 1 WHERE id = 1";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            ConfigFiscal c = obterConfiguracao();
            return c.getUltimoNumeroNfe();
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao incrementar número NF-e: " + e.getMessage());
            return 1;
        }
    }
}
