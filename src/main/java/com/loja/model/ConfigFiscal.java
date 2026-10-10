package com.loja.model;

import com.loja.util.FiscalUtils;

public class ConfigFiscal {
    private int id = 1;
    private String cnpj = "";
    private String inscricaoEstadual = "";
    private String razaoSocial = "";
    private String nomeFantasia = "";
    private int regimeTributario = 1; // 1: Simples Nacional, 2: Simples Excesso Sublimite, 3: Regime Normal
    private int ambiente = 2; // 1: Produção, 2: Homologação / Testes
    private String cscToken = "";
    private String cscId = "000001";
    private int serieNfce = 1;
    private int ultimoNumeroNfce = 0;
    private int serieNfe = 1;
    private int ultimoNumeroNfe = 0;
    private String certificadoCaminho = "";
    private String certificadoSenha = "";

    // Endereço Fiscal da Empresa
    private String logradouro = "";
    private String numero = "";
    private String complemento = "";
    private String bairro = "";
    private String municipio = "";
    private String codigoIbge = "";
    private String uf = "SP";
    private String cep = "";
    private String telefone = "";

    public ConfigFiscal() {}

    public boolean isHomologacao() {
        return ambiente == 2;
    }

    public String getDescricaoAmbiente() {
        return ambiente == 1 ? "Produção (Notas Reais com Valor Fiscal)" : "Homologação (Ambiente de Testes da SEFAZ)";
    }

    public String getDescricaoRegime() {
        switch (regimeTributario) {
            case 1: return "Simples Nacional (ME / EPP / MEI)";
            case 2: return "Simples Nacional - Excesso de Sublimite";
            case 3: return "Regime Normal (Lucro Presumido / Real)";
            default: return "Simples Nacional";
        }
    }

    public boolean isConfiguradaParaEmissao() {
        return !FiscalUtils.apenasNumeros(cnpj).isEmpty() &&
               !razaoSocial.trim().isEmpty() &&
               !inscricaoEstadual.trim().isEmpty();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj != null ? cnpj : ""; }

    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual != null ? inscricaoEstadual : ""; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial != null ? razaoSocial : ""; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia != null ? nomeFantasia : ""; }

    public int getRegimeTributario() { return regimeTributario; }
    public void setRegimeTributario(int regimeTributario) { this.regimeTributario = regimeTributario; }

    public int getAmbiente() { return ambiente; }
    public void setAmbiente(int ambiente) { this.ambiente = ambiente; }

    public String getCscToken() { return cscToken; }
    public void setCscToken(String cscToken) { this.cscToken = cscToken != null ? cscToken : ""; }

    public String getCscId() { return cscId; }
    public void setCscId(String cscId) { this.cscId = cscId != null ? cscId : "000001"; }

    public int getSerieNfce() { return serieNfce; }
    public void setSerieNfce(int serieNfce) { this.serieNfce = serieNfce; }

    public int getUltimoNumeroNfce() { return ultimoNumeroNfce; }
    public void setUltimoNumeroNfce(int ultimoNumeroNfce) { this.ultimoNumeroNfce = ultimoNumeroNfce; }

    public int getSerieNfe() { return serieNfe; }
    public void setSerieNfe(int serieNfe) { this.serieNfe = serieNfe; }

    public int getUltimoNumeroNfe() { return ultimoNumeroNfe; }
    public void setUltimoNumeroNfe(int ultimoNumeroNfe) { this.ultimoNumeroNfe = ultimoNumeroNfe; }

    public String getCertificadoCaminho() { return certificadoCaminho; }
    public void setCertificadoCaminho(String certificadoCaminho) { this.certificadoCaminho = certificadoCaminho != null ? certificadoCaminho : ""; }

    public String getCertificadoSenha() { return certificadoSenha; }
    public void setCertificadoSenha(String certificadoSenha) { this.certificadoSenha = certificadoSenha != null ? certificadoSenha : ""; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro != null ? logradouro : ""; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero != null ? numero : ""; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento != null ? complemento : ""; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro != null ? bairro : ""; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio != null ? municipio : ""; }

    public String getCodigoIbge() { return codigoIbge; }
    public void setCodigoIbge(String codigoIbge) { this.codigoIbge = codigoIbge != null ? codigoIbge : ""; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf != null ? uf.toUpperCase() : "SP"; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep != null ? cep : ""; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone != null ? telefone : ""; }
}
