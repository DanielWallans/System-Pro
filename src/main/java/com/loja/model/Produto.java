package com.loja.model;

public class Produto {
    private int id;
    private String codigoBarras;
    private String nome;
    private double precoCusto;
    private double preco;
    private int estoque;

    // Campos Fiscais Tributários (SEFAZ / NF-e / NFC-e)
    private String ncm;       // Nomenclatura Comum do Mercosul (8 dígitos)
    private String cest;      // Código Especificador da Substituição Tributária (7 dígitos)
    private String cfop;      // Código Fiscal de Operações (ex: 5102, 5405)
    private int origem;       // 0 - Nacional, 1 - Importação direta, etc.
    private String csosnCst;  // Código de Situação Tributária (Simples Nacional: 102, 500, etc.)
    private String unidade;   // UN, PC, CX, PAR, KG, MT

    public Produto(int id, String nome, double preco, int estoque) {
        this(id, "", nome, 0.0, preco, estoque, "", "", "5102", 0, "102", "UN");
    }

    public Produto(int id, String codigoBarras, String nome, double preco, int estoque) {
        this(id, codigoBarras, nome, 0.0, preco, estoque, "", "", "5102", 0, "102", "UN");
    }

    public Produto(int id, String codigoBarras, String nome, double precoCusto, double preco, int estoque,
                   String ncm, String cest, String cfop, int origem, String csosnCst, String unidade) {
        this.id = id;
        this.codigoBarras = codigoBarras != null ? codigoBarras : "";
        this.nome = nome;
        this.precoCusto = precoCusto;
        this.preco = preco;
        this.estoque = estoque;
        this.ncm = ncm != null ? ncm : "";
        this.cest = cest != null ? cest : "";
        this.cfop = (cfop != null && !cfop.trim().isEmpty()) ? cfop.trim() : "5102";
        this.origem = origem;
        this.csosnCst = (csosnCst != null && !csosnCst.trim().isEmpty()) ? csosnCst.trim() : "102";
        this.unidade = (unidade != null && !unidade.trim().isEmpty()) ? unidade.trim() : "UN";
    }

    public boolean temEstoque(int quantidade) {
        return this.estoque >= quantidade;
    }

    public void baixarEstoque(int quantidade) {
        if (temEstoque(quantidade)) {
            this.estoque -= quantidade;
        }
    }

    public double getMargemLucroPercentual() {
        if (precoCusto <= 0) return 0.0;
        return ((preco - precoCusto) / precoCusto) * 100.0;
    }

    public double getLucroBrutoUnitario() {
        return preco - precoCusto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public double getPrecoCusto() { return precoCusto; }
    public void setPrecoCusto(double precoCusto) { this.precoCusto = precoCusto; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public int getEstoque() { return estoque; }
    public void setEstoque(int estoque) { this.estoque = estoque; }

    public String getNcm() { return ncm; }
    public void setNcm(String ncm) { this.ncm = ncm; }

    public String getCest() { return cest; }
    public void setCest(String cest) { this.cest = cest; }

    public String getCfop() { return cfop; }
    public void setCfop(String cfop) { this.cfop = cfop; }

    public int getOrigem() { return origem; }
    public void setOrigem(int origem) { this.origem = origem; }

    public String getCsosnCst() { return csosnCst; }
    public void setCsosnCst(String csosnCst) { this.csosnCst = csosnCst; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    @Override
    public String toString() {
        return (codigoBarras != null && !codigoBarras.isEmpty() ? "[" + codigoBarras + "] " : "#" + id + " - ") + nome + " (R$ " + String.format("%.2f", preco) + ")";
    }
}
