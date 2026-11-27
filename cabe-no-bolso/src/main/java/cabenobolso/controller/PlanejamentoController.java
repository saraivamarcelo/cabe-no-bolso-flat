package cabenobolso.controller;

import cabenobolso.model.Despesa;
import cabenobolso.model.Participante;
import cabenobolso.model.Planejamento;
import cabenobolso.model.RegraNegocioException;
import cabenobolso.model.ResultadoSimulacao;
import cabenobolso.model.Resumo;
import cabenobolso.model.Simulador;
import cabenobolso.model.TipoDespesa;
import cabenobolso.persistence.PersistenciaException;
import cabenobolso.persistence.RepositorioPlanejamento;
import cabenobolso.util.CasoReferencia;
import cabenobolso.util.CenarioEquipe;
import cabenobolso.util.Dinheiro;

import java.io.File;
import java.util.List;
import java.util.function.Function;

/**
 * Controlador (MVC): recebe as ações da visão (textos digitados, itens
 * selecionados), converte as entradas, encaminha ao modelo e devolve
 * resultados ou deixa subir as exceções com mensagens claras.
 *
 * Não conhece Swing nem a classe concreta de persistência: depende apenas da
 * interface {@link RepositorioPlanejamento}, criada por uma fábrica injetada.
 */
public class PlanejamentoController {

    private Planejamento planejamento = new Planejamento("Novo planejamento");
    private final Function<File, RepositorioPlanejamento> fabricaRepositorio;

    public PlanejamentoController(Function<File, RepositorioPlanejamento> fabricaRepositorio) {
        this.fabricaRepositorio = fabricaRepositorio;
    }

    // ------------------------------------------------------------------ planejamento

    public String getNome() {
        return planejamento.getNome();
    }

    public void novoPlanejamento(String nome) {
        this.planejamento = new Planejamento(nome);
    }

    public void usarCasoReferencia() {
        this.planejamento = CasoReferencia.criar();
    }

    public void usarCenarioEquipe() {
        this.planejamento = CenarioEquipe.criar();
    }

    // ------------------------------------------------------------------ participantes

    public void cadastrarParticipante(String codigo, String nome, String orcamento) throws RegraNegocioException {
        planejamento.cadastrarParticipante(lerCodigo(codigo), nome, Dinheiro.parse(orcamento));
    }

    public void alterarParticipante(String codigo, String nome, String orcamento) throws RegraNegocioException {
        planejamento.alterarParticipante(lerCodigo(codigo), nome, Dinheiro.parse(orcamento));
    }

    public void excluirParticipante(String codigo) throws RegraNegocioException {
        planejamento.excluirParticipante(lerCodigo(codigo));
    }

    public List<Participante> participantes() {
        return planejamento.getParticipantes();
    }

    // ------------------------------------------------------------------ despesas

    public void incluirDespesa(TipoDespesa tipo, String descricao, String valor, List<Integer> codigos,
                               boolean essencial, int prioridade) throws RegraNegocioException {
        planejamento.incluirDespesa(tipo, descricao, Dinheiro.parse(valor), codigos, essencial, prioridade);
    }

    public void editarDespesa(int indice, TipoDespesa tipo, String descricao, String valor, List<Integer> codigos,
                              boolean essencial, int prioridade) throws RegraNegocioException {
        planejamento.editarDespesa(indice, tipo, descricao, Dinheiro.parse(valor), codigos, essencial, prioridade);
    }

    public void excluirDespesa(int indice) throws RegraNegocioException {
        planejamento.excluirDespesa(indice);
    }

    public List<Despesa> despesas() {
        return planejamento.getDespesas();
    }

    // ------------------------------------------------------------------ resumo e simulação

    public Resumo resumo() {
        return planejamento.resumo();
    }

    public ResultadoSimulacao simular() throws RegraNegocioException {
        return Simulador.simular(planejamento);
    }

    // ------------------------------------------------------------------ persistência

    public void salvar(File arquivo) throws PersistenciaException {
        fabricaRepositorio.apply(arquivo).salvar(planejamento);
    }

    /**
     * Carrega de um arquivo. O planejamento em uso só é substituído se a leitura
     * terminar com sucesso; em caso de falha a exceção sobe e nada muda.
     */
    public void carregar(File arquivo) throws PersistenciaException {
        Planejamento lido = fabricaRepositorio.apply(arquivo).carregar();
        this.planejamento = lido;
    }

    // ------------------------------------------------------------------ utilitários

    private static int lerCodigo(String texto) throws RegraNegocioException {
        if (texto == null || texto.trim().isEmpty()) {
            throw new RegraNegocioException("Informe o código do participante.");
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Código inválido: informe um número inteiro positivo.");
        }
    }
}
