package cabenobolso.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Raiz do modelo: representa uma atividade em grupo com seus participantes e
 * despesas. Toda operação valida antes de alterar, de modo que uma entrada
 * inválida nunca deixa o planejamento em estado parcial.
 *
 * Coleções escolhidas:
 * - participantes: Map&lt;Integer, Participante&gt; (TreeMap) - busca por código em
 *   O(log n), unicidade do código garantida pela chave e iteração em ordem
 *   crescente de código (necessária para distribuir os centavos).
 * - despesas: List&lt;Despesa&gt; (ArrayList) - mantém a ordem de inclusão e permite
 *   editar/excluir por posição.
 */
public class Planejamento implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nome;
    // Declaradas com o tipo concreto (TreeMap/ArrayList), que são Serializable.
    private final TreeMap<Integer, Participante> participantes = new TreeMap<>();
    private final ArrayList<Despesa> despesas = new ArrayList<>();

    public Planejamento(String nome) {
        this.nome = (nome == null || nome.trim().isEmpty()) ? "Novo planejamento" : nome.trim();
    }

    public String getNome() {
        return nome;
    }

    // ------------------------------------------------------------------ participantes

    public Participante cadastrarParticipante(int codigo, String nomeParticipante, long orcamento)
            throws RegraNegocioException {
        if (participantes.containsKey(codigo)) {
            throw new RegraNegocioException("Já existe um participante com o código " + codigo + ".");
        }
        Participante novo = new Participante(codigo, nomeParticipante, orcamento); // valida
        participantes.put(codigo, novo);
        return novo;
    }

    public void alterarParticipante(int codigo, String novoNome, long novoOrcamento)
            throws RegraNegocioException {
        Participante p = exigirParticipante(codigo);
        p.atualizar(novoNome, novoOrcamento);
    }

    public void excluirParticipante(int codigo) throws RegraNegocioException {
        Participante p = exigirParticipante(codigo);
        for (Despesa d : despesas) {
            if (d.getParticipantes().contains(p)) {
                throw new RegraNegocioException("Não é possível excluir " + p.getNome()
                        + ": participa da despesa \"" + d.getDescricao() + "\".");
            }
        }
        participantes.remove(codigo);
    }

    public Participante buscarParticipante(int codigo) {
        return participantes.get(codigo);
    }

    private Participante exigirParticipante(int codigo) throws RegraNegocioException {
        Participante p = participantes.get(codigo);
        if (p == null) {
            throw new RegraNegocioException("Não existe participante com o código " + codigo + ".");
        }
        return p;
    }

    /** Participantes em ordem crescente de código (visão somente leitura). */
    public List<Participante> getParticipantes() {
        return Collections.unmodifiableList(new ArrayList<>(participantes.values()));
    }

    // ------------------------------------------------------------------ despesas

    public Despesa incluirDespesa(TipoDespesa tipo, String descricao, long valor,
                                  Collection<Integer> codigos, boolean essencial, int prioridade)
            throws RegraNegocioException {
        Despesa nova = criarDespesa(tipo, descricao, valor, codigos, essencial, prioridade);
        despesas.add(nova);
        return nova;
    }

    public Despesa editarDespesa(int indice, TipoDespesa tipo, String descricao, long valor,
                                 Collection<Integer> codigos, boolean essencial, int prioridade)
            throws RegraNegocioException {
        exigirIndice(indice);
        Despesa nova = criarDespesa(tipo, descricao, valor, codigos, essencial, prioridade);
        despesas.set(indice, nova); // só substitui depois de tudo validado
        return nova;
    }

    public void excluirDespesa(int indice) throws RegraNegocioException {
        exigirIndice(indice);
        despesas.remove(indice);
    }

    private void exigirIndice(int indice) throws RegraNegocioException {
        if (indice < 0 || indice >= despesas.size()) {
            throw new RegraNegocioException("Despesa inexistente. Selecione uma despesa da lista.");
        }
    }

    /** Fábrica: decide a subclasse concreta conforme o tipo escolhido. */
    private Despesa criarDespesa(TipoDespesa tipo, String descricao, long valor,
                                 Collection<Integer> codigos, boolean essencial, int prioridade)
            throws RegraNegocioException {
        if (tipo == null) {
            throw new RegraNegocioException("Informe o tipo da despesa.");
        }
        List<Participante> selecionados = resolverParticipantes(codigos);
        switch (tipo) {
            case FIXA:
                return new DespesaFixa(descricao, valor, selecionados, essencial, prioridade);
            case POR_PESSOA:
                return new DespesaPorPessoa(descricao, valor, selecionados, essencial, prioridade);
            default:
                throw new RegraNegocioException("Tipo de despesa não suportado.");
        }
    }

    private List<Participante> resolverParticipantes(Collection<Integer> codigos) throws RegraNegocioException {
        if (codigos == null || codigos.isEmpty()) {
            throw new RegraNegocioException("A despesa deve ter pelo menos um participante.");
        }
        Set<Integer> vistos = new HashSet<>();
        List<Participante> resultado = new ArrayList<>();
        for (Integer codigo : codigos) {
            if (!vistos.add(codigo)) {
                throw new RegraNegocioException("Participante repetido na despesa: código " + codigo + ".");
            }
            resultado.add(exigirParticipante(codigo));
        }
        return resultado;
    }

    /** Despesas na ordem de inclusão (visão somente leitura). */
    public List<Despesa> getDespesas() {
        return Collections.unmodifiableList(despesas);
    }

    // ------------------------------------------------------------------ resumo

    /** Recalcula custo total, totais individuais e situação de cada participante. */
    public Resumo resumo() {
        return Resumo.calcular(participantes.values(), despesas);
    }
}
