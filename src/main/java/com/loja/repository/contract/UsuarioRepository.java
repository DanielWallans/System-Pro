package com.loja.repository.contract;

import com.loja.model.Usuario;
import java.util.List;

public interface UsuarioRepository {
    Usuario autenticar(String login, String senhaPlana);
    List<Usuario> listarTodos();
    Usuario buscarPorId(int id);
    boolean salvar(Usuario u, String senhaPlana);
    boolean atualizar(Usuario u);
    boolean alterarSenha(int id, String novaSenhaPlana);
    boolean alternarStatus(int id, boolean ativo);
    boolean excluir(int id);
}

