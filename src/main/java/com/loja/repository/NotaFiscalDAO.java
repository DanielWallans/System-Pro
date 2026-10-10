package com.loja.repository;

import com.loja.model.NotaFiscal;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotaFiscalDAO {

    public boolean salvar(NotaFiscal nf) {
        String sql = "INSERT INTO nota_fiscal (tipo, modelo, serie, numero, chave_acesso, status, " +
                     "data_emissao, venda_id, os_id, cliente_id, cliente_cpf_cnpj, cliente_nome, " +
                     "valor_total, protocolo_autorizacao, motivo_status, qr_code_url, xml_envio, xml_retorno) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nf.getTipo());
            stmt.setString(2, nf.getModelo());
            stmt.setInt(3, nf.getSerie());
            stmt.setInt(4, nf.getNumero());
            stmt.setString(5, nf.getChaveAcesso());
            stmt.setString(6, nf.getStatus());
            stmt.setTimestamp(7, Timestamp.valueOf(nf.getDataEmissao() != null ? nf.getDataEmissao() : LocalDateTime.now()));

            if (nf.getVendaId() != null) stmt.setInt(8, nf.getVendaId()); else stmt.setNull(8, Types.INTEGER);
            if (nf.getOsId() != null) stmt.setInt(9, nf.getOsId()); else stmt.setNull(9, Types.INTEGER);
            if (nf.getClienteId() != null) stmt.setInt(10, nf.getClienteId()); else stmt.setNull(10, Types.INTEGER);

            stmt.setString(11, nf.getClienteCpfCnpj());
            stmt.setString(12, nf.getClienteNome());
            stmt.setDouble(13, nf.getValorTotal());
            stmt.setString(14, nf.getProtocoloAutorizacao());
            stmt.setString(15, nf.getMotivoStatus());
            stmt.setString(16, nf.getQrCodeUrl());
            stmt.setString(17, nf.getXmlEnvio());
            stmt.setString(18, nf.getXmlRetorno());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    nf.setId(rs.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao gravar nota fiscal: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizarStatus(int id, String status, String protocolo, String motivo, String chave, String qrCode, String xmlRetorno) {
        String sql = "UPDATE nota_fiscal SET status = ?, protocolo_autorizacao = ?, motivo_status = ?, chave_acesso = ?, qr_code_url = ?, xml_retorno = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, protocolo);
            stmt.setString(3, motivo);
            stmt.setString(4, chave);
            stmt.setString(5, qrCode);
            stmt.setString(6, xmlRetorno);
            stmt.setInt(7, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao atualizar status da nota fiscal #" + id + ": " + e.getMessage());
            return false;
        }
    }

    public List<NotaFiscal> listarRecentes(int limite) {
        List<NotaFiscal> lista = new ArrayList<>();
        String sql = "SELECT * FROM nota_fiscal ORDER BY id DESC LIMIT ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearNotaFiscal(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar notas fiscais recentes: " + e.getMessage());
        }
        return lista;
    }

    public NotaFiscal buscarPorVenda(int vendaId) {
        String sql = "SELECT * FROM nota_fiscal WHERE venda_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vendaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearNotaFiscal(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar nota fiscal por venda: " + e.getMessage());
        }
        return null;
    }

    private NotaFiscal mapearNotaFiscal(ResultSet rs) throws SQLException {
        NotaFiscal nf = new NotaFiscal();
        nf.setId(rs.getInt("id"));
        nf.setTipo(rs.getString("tipo"));
        nf.setModelo(rs.getString("modelo"));
        nf.setSerie(rs.getInt("serie"));
        nf.setNumero(rs.getInt("numero"));
        nf.setChaveAcesso(rs.getString("chave_acesso"));
        nf.setStatus(rs.getString("status"));

        Timestamp ts = rs.getTimestamp("data_emissao");
        if (ts != null) {
            nf.setDataEmissao(ts.toLocalDateTime());
        }

        int vid = rs.getInt("venda_id");
        if (!rs.wasNull()) nf.setVendaId(vid);

        int oid = rs.getInt("os_id");
        if (!rs.wasNull()) nf.setOsId(oid);

        int cid = rs.getInt("cliente_id");
        if (!rs.wasNull()) nf.setClienteId(cid);

        nf.setClienteCpfCnpj(rs.getString("cliente_cpf_cnpj"));
        nf.setClienteNome(rs.getString("cliente_nome"));
        nf.setValorTotal(rs.getDouble("valor_total"));
        nf.setProtocoloAutorizacao(rs.getString("protocolo_autorizacao"));
        nf.setMotivoStatus(rs.getString("motivo_status"));
        nf.setQrCodeUrl(rs.getString("qr_code_url"));
        nf.setXmlEnvio(rs.getString("xml_envio"));
        nf.setXmlRetorno(rs.getString("xml_retorno"));
        return nf;
    }
}
