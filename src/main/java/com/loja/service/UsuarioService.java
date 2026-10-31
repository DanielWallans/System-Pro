package com.loja.service;

import com.loja.exception.BusinessRuleException;
import com.loja.exception.EntityNotFoundException;
import com.loja.exception.ValidationException;
import com.loja.model.Usuario;
import com.loja.repository.contract.UsuarioRepository;
import com.loja.util.AppLogger;

import java.util.List;

/**
 * Camada de Serviço responsável pela gestão de Usuários, Autenticação e Perfis de Acesso.
 */
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario autenticar(String login, String senhaPlana) {
        if (login == null || login.trim().isEmpty()) {
            throw new ValidationException("Informe o login de usuário.");
        }
        if (senhaPlana == null || senhaPlana.isEmpty()) {
            throw new ValidationException("Informe a senha de acesso.");
        }

        Usuario usuario = usuarioRepository.autenticar(login.trim(), senhaPlana);
        if (usuario == null) {
            AppLogger.warn(UsuarioService.class, "Tentativa de login inválida para o usuário: " + login);
            return null;
        }

        AppLogger.info(UsuarioService.class, "Usuário autenticado com sucesso: " + usuario.getNome() + " (" + usuario.getPerfil() + ")");
        return usuario;
    }

    public void cadastrarUsuario(Usuario usuario, String senhaPlana) {
        if (usuario == null) {
            throw new ValidationException("Dados de usuário inválidos.");
        }
        if (usuario.getNome() == null || usuario.getNome().trim().length() < 3) {
            throw new ValidationException("O nome completo deve ter ao menos 3 caracteres.");
        }
        if (usuario.getLogin() == null || usuario.getLogin().trim().length() < 3) {
            throw new ValidationException("O login de acesso deve ter ao menos 3 caracteres.");
        }
        if (senhaPlana == null || senhaPlana.length() < 4) {
            throw new ValidationException("A senha deve conter no mínimo 4 caracteres.");
        }

        boolean ok = usuarioRepository.salvar(usuario, senhaPlana);
        if (!ok) {
            throw new BusinessRuleException("Não foi possível cadastrar o usuário. Verifique se o login já existe.");
        }
        AppLogger.info(UsuarioService.class, "Novo operador cadastrado: " + usuario.getLogin());
    }

    public void alterarSenha(int id, String novaSenhaPlana) {
        if (novaSenhaPlana == null || novaSenhaPlana.length() < 4) {
            throw new ValidationException("A nova senha deve ter no mínimo 4 caracteres.");
        }
        boolean ok = usuarioRepository.alterarSenha(id, novaSenhaPlana);
        if (!ok) {
            throw new BusinessRuleException("Falha ao alterar a senha do usuário.");
        }
        AppLogger.info(UsuarioService.class, "Senha alterada para o usuário ID: " + id);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.listarTodos();
    }

    public Usuario buscarPorId(int id) {
        Usuario u = usuarioRepository.buscarPorId(id);
        if (u == null) {
            throw new EntityNotFoundException("Usuário", id);
        }
        return u;
    }

    public void alternarStatus(int id, boolean ativo) {
        boolean ok = usuarioRepository.alternarStatus(id, ativo);
        if (!ok) {
            throw new BusinessRuleException("Falha ao alterar o status do usuário.");
        }
    }
}

