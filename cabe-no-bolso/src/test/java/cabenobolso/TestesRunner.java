package cabenobolso;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.model.Despesa;
import cabenobolso.model.Participante;
import cabenobolso.model.Planejamento;
import cabenobolso.model.RegraNegocioException;
import cabenobolso.model.ResultadoSimulacao;
import cabenobolso.model.Resumo;
import cabenobolso.model.TipoDespesa;
import cabenobolso.persistence.ArquivoPlanejamentoRepository;
import cabenobolso.persistence.PersistenciaException;
import cabenobolso.util.CasoReferencia;
import cabenobolso.util.CenarioEquipe;
import cabenobolso.util.Dinheiro;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Testes manuais automatizados (sem dependências externas). Cada cenário registra
 * entrada, resultado esperado, resultado obtido e situação, e o programa imprime
 * uma tabela em Markdown que serve de evidência para o relatório.
 *
 * Uso: java -cp out cabenobolso.TestesRunner [arquivo-de-evidencias.md]
 * Código de saída 0 se todos passarem; 1 caso contrário.
 */
public final class TestesRunner {

    private static final class Resultado {
        final String cenario;
        final String entrada;
        final String esperado;
        final String obtido;
        final boolean ok;

        Resultado(String cenario, String entrada, String esperado, String obtido, boolean ok) {
            this.cenario = cenario;
            this.entrada = entrada;
            this.esperado = esperado;
            this.obtido = obtido;
            this.ok = ok;
        }
    }

    /** Teste que pode lançar qualquer exceção (tratada como falha). */
    private interface Teste {
        void executar() throws Exception;
    }

    private static final List<Resultado> RESULTADOS = new ArrayList<>();

    private TestesRunner() {
    }

    // ------------------------------------------------------------------ infraestrutura

    private static void registrar(String cenario, String entrada, String esperado, String obtido) {
        RESULTADOS.add(new Resultado(cenario, entrada, esperado, obtido, esperado.equals(obtido)));
    }

    private static void rodar(String cenario, Teste teste) {
        try {
            teste.executar();
        } catch (Exception | AssertionError e) {
            RESULTADOS.add(new Resultado(cenario, "-", "sem exceção inesperada",
                    "EXCEÇÃO: " + e, false));
        }
    }

    private static String reais(long centavos) {
        return Dinheiro.formatar(centavos);
    }

    private static String totais(Resumo r) {
        StringBuilder sb = new StringBuilder();
        for (Resumo.Linha l : r.getLinhas()) {
            sb.append(l.getNome()).append('=').append(reais(l.getTotal())).append("; ");
        }
        return sb.append("custo total=").append(reais(r.getCustoTotal())).toString();
    }

    /** Descrição completa do estado, usada para provar que nada mudou após uma recusa. */
    private static String estado(PlanejamentoController c) {
        StringBuilder sb = new StringBuilder();
        for (Participante p : c.participantes()) {
            sb.append(p.getCodigo()).append(':').append(p.getNome()).append(':').append(p.getOrcamento()).append('|');
        }
        for (Despesa d : c.despesas()) {
            sb.append(d.getDescricao()).append(':').append(d.getTipo()).append(':').append(d.getValor())
                    .append(':').append(d.getParticipantes()).append('|');
        }
        return sb.toString();
    }

    private static PlanejamentoController controladorComCaso() {
        PlanejamentoController c = new PlanejamentoController(ArquivoPlanejamentoRepository::new);
        c.usarCasoReferencia();
        return c;
    }

    // ------------------------------------------------------------------ cenários

    /** Seção 4: caso de referência (valores por participante, custo total e situação). */
    private static void casoReferencia() throws Exception {
        PlanejamentoController c = controladorComCaso();
        Resumo r = c.resumo();
        registrar("Caso de referência - totais",
                "Ana(70), Bruno(60), Carla(70); Transporte fixa 90 (1,2,3); Almoço p/pessoa 20 (1,2,3); "
                        + "Atividade fixa 30 (1,3); Sobremesa p/pessoa 10 (1,2,3)",
                "Ana=R$ 75,00; Bruno=R$ 60,00; Carla=R$ 75,00; custo total=R$ 210,00",
                totais(r));

        StringBuilder matriz = new StringBuilder();
        for (Resumo.Linha l : r.getLinhas()) {
            matriz.append(l.getNome()).append(l.getPorDespesa()).append(' ');
        }
        registrar("Caso de referência - rateio por despesa",
                "Mesma entrada (ordem: Transporte, Almoço, Atividade, Sobremesa; valores em centavos)",
                "Ana[3000, 2000, 1500, 1000] Bruno[3000, 2000, 0, 1000] Carla[3000, 2000, 1500, 1000] ",
                matriz.toString());

        StringBuilder situacao = new StringBuilder();
        for (Resumo.Linha l : r.getLinhas()) {
            situacao.append(l.getNome()).append(l.isDentroDoOrcamento() ? " dentro" : " excede " + reais(l.getExcesso()))
                    .append("; ");
        }
        registrar("Caso de referência - situação no orçamento",
                "Mesma entrada",
                "Ana excede R$ 5,00; Bruno dentro; Carla excede R$ 5,00; ",
                situacao.toString());
    }

    /** Teste adicional 1: centavos no rateio. */
    private static void centavos() throws Exception {
        Planejamento p = new Planejamento("centavos");
        p.cadastrarParticipante(1, "A", 100000);
        p.cadastrarParticipante(2, "B", 100000);
        p.cadastrarParticipante(3, "C", 100000);
        Despesa d = p.incluirDespesa(TipoDespesa.FIXA, "Aluguel", 10000, Arrays.asList(3, 1, 2), true, 0);
        long soma = 0;
        StringBuilder sb = new StringBuilder();
        for (long v : d.ratear().values()) {
            sb.append(reais(v)).append(", ");
            soma += v;
        }
        registrar("Centavos no rateio",
                "Despesa fixa de R$ 100,00 para os códigos 1, 2 e 3 (informados fora de ordem: 3, 1, 2)",
                "R$ 33,34, R$ 33,33, R$ 33,33, soma=R$ 100,00",
                sb.toString() + "soma=" + reais(soma));

        registrar("Centavos - subconjunto (códigos 2 e 3, R$ 10,01)",
                "Despesa fixa de R$ 10,01 para os códigos 2 e 3 (informados fora de ordem: 3, 2)",
                "código 2 paga R$ 5,01; código 3 paga R$ 5,00",
                tentarFixa(p));
    }

    private static String tentarFixa(Planejamento p) throws RegraNegocioException {
        Despesa d = p.incluirDespesa(TipoDespesa.FIXA, "Taxa 2", 1001, Arrays.asList(3, 2), true, 0);
        return "código 2 paga " + reais(d.contribuicaoDe(p.buscarParticipante(2)))
                + "; código 3 paga " + reais(d.contribuicaoDe(p.buscarParticipante(3)));
    }

    /** Teste adicional 2: entrada ou vínculo inválido, sem alterar os dados. */
    private static void entradasInvalidas() throws Exception {
        PlanejamentoController c = controladorComCaso();
        String antes = estado(c);
        Resumo resumoAntes = c.resumo();

        List<String> casos = new ArrayList<>();
        List<String> obtido = new ArrayList<>();
        List<String> esperado = new ArrayList<>();

        String[][] despesas = {
                {"sem participantes", "Passeio", "50,00", ""},
                {"valor negativo", "Passeio", "-10,00", "1"},
                {"valor zero", "Passeio", "0", "1"},
                {"valor vazio", "Passeio", "", "1"},
                {"descrição vazia", "  ", "10,00", "1"},
                {"valor não numérico", "Passeio", "abc", "1"},
                {"mais de 2 casas decimais", "Passeio", "10,999", "1"},
                {"participante inexistente", "Passeio", "10,00", "99"},
                {"participante repetido", "Passeio", "10,00", "1,1"},
        };
        for (String[] caso : despesas) {
            List<Integer> codigos = new ArrayList<>();
            for (String parte : caso[3].split(",")) {
                if (!parte.trim().isEmpty()) {
                    codigos.add(Integer.parseInt(parte.trim()));
                }
            }
            casos.add("despesa " + caso[0]);
            esperado.add("recusada");
            try {
                c.incluirDespesa(TipoDespesa.FIXA, caso[1], caso[2], codigos, true, 0);
                obtido.add("ACEITA (erro)");
            } catch (RegraNegocioException e) {
                obtido.add(e.getMessage() != null && !e.getMessage().isEmpty() ? "recusada" : "recusada sem mensagem");
            }
        }
        String[][] participantes = {
                {"código duplicado", "1", "Outro", "10,00"},
                {"nome vazio", "9", "", "10,00"},
                {"orçamento negativo", "9", "Dani", "-1,00"},
                {"orçamento vazio", "9", "Dani", ""},
                {"código não numérico", "x", "Dani", "10,00"},
                {"código vazio", "", "Dani", "10,00"},
        };
        for (String[] caso : participantes) {
            casos.add("participante " + caso[0]);
            esperado.add("recusado");
            try {
                c.cadastrarParticipante(caso[1], caso[2], caso[3]);
                obtido.add("ACEITO (erro)");
            } catch (RegraNegocioException e) {
                obtido.add(e.getMessage() != null && !e.getMessage().isEmpty() ? "recusado" : "recusado sem mensagem");
            }
        }
        registrar("Entrada ou vínculo inválido - recusa com mensagem",
                String.join("; ", casos), String.join(", ", esperado), String.join(", ", obtido));
        registrar("Entrada ou vínculo inválido - dados inalterados",
                "Estado completo antes e depois das 15 tentativas inválidas",
                "estado idêntico e totais idênticos",
                antes.equals(estado(c)) && totais(resumoAntes).equals(totais(c.resumo()))
                        ? "estado idêntico e totais idênticos" : "ESTADO ALTERADO");

        // Mensagem ao usuário deve ser clara (exemplo exibido como evidência).
        String exemplo = "";
        try {
            c.incluirDespesa(TipoDespesa.FIXA, "Passeio", "50,00", Collections.<Integer>emptyList(), true, 0);
        } catch (RegraNegocioException e) {
            exemplo = e.getMessage();
        }
        registrar("Entrada inválida - mensagem exibida",
                "Despesa sem participantes",
                "A despesa deve ter pelo menos um participante.", exemplo);
    }

    /** Teste adicional 3: alteração de orçamento. */
    private static void alteracaoOrcamento() throws Exception {
        PlanejamentoController c = controladorComCaso();
        c.alterarParticipante("1", "Ana", "80,00");
        Resumo.Linha ana = c.resumo().getLinha(1);
        registrar("Alteração de orçamento",
                "Orçamento de Ana alterado de R$ 70,00 para R$ 80,00 no caso principal",
                "total de Ana=R$ 75,00; dentro do orçamento; excesso=R$ 0,00",
                "total de Ana=" + reais(ana.getTotal()) + "; "
                        + (ana.isDentroDoOrcamento() ? "dentro do orçamento" : "excede") + "; excesso=" + reais(ana.getExcesso()));
        c.alterarParticipante("1", "Ana Paula", "80,00");
        registrar("Alteração de nome",
                "Nome do código 1 alterado para \"Ana Paula\"; a despesa vinculada deve refletir o novo nome",
                "Ana Paula",
                c.despesas().get(0).getParticipantes().first().getNome());
    }

    /** Teste adicional 4: exclusão de despesa. */
    private static void exclusaoDespesa() throws Exception {
        PlanejamentoController c = controladorComCaso();
        c.excluirDespesa(2); // índice 2 = Atividade recreativa
        Resumo r = c.resumo();
        registrar("Exclusão de despesa",
                "Excluir \"Atividade recreativa\" do caso principal original",
                "Ana=R$ 60,00; Bruno=R$ 60,00; Carla=R$ 60,00; custo total=R$ 180,00",
                totais(r));
    }

    /** Teste adicional 5: salvar e reiniciar. */
    private static void salvarEReiniciar() throws Exception {
        File arquivo = File.createTempFile("cabenobolso-teste", ".dat");
        try {
            PlanejamentoController original = controladorComCaso();
            original.salvar(arquivo);

            PlanejamentoController reiniciado = new PlanejamentoController(ArquivoPlanejamentoRepository::new);
            reiniciado.carregar(arquivo);
            reiniciado.carregar(arquivo); // carregar duas vezes não pode duplicar nada

            registrar("Salvar e reiniciar - dados e vínculos",
                    "Salvar o caso principal, iniciar um controlador novo e carregar (duas vezes)",
                    "3 participantes, 4 despesas, " + totais(original.resumo()),
                    reiniciado.participantes().size() + " participantes, " + reiniciado.despesas().size()
                            + " despesas, " + totais(reiniciado.resumo()));
            registrar("Salvar e reiniciar - estado completo idêntico",
                    "Comparação de participantes, orçamentos, despesas e vínculos",
                    estado(original), estado(reiniciado));

            // Vínculos: a despesa deve apontar para o MESMO objeto Participante da lista.
            Participante naLista = reiniciado.participantes().get(0);
            Participante naDespesa = reiniciado.despesas().get(0).getParticipantes().first();
            registrar("Salvar e reiniciar - vínculos preservados (mesmo objeto)",
                    "Participante de código 1 na lista x participante na primeira despesa",
                    "true", String.valueOf(naLista == naDespesa));

            reiniciado.alterarParticipante("1", "Ana", "90,00");
            registrar("Salvar e reiniciar - alteração reflete nas despesas",
                    "Após carregar, alterar o orçamento de Ana para R$ 90,00",
                    "Ana dentro do orçamento", reiniciado.resumo().getLinha(1).isDentroDoOrcamento()
                            ? "Ana dentro do orçamento" : "Ana excede");
        } finally {
            arquivo.delete();
        }
    }

    /** Teste adicional 6: problemas com arquivo. */
    private static void problemasArquivo() throws Exception {
        PlanejamentoController c = controladorComCaso();
        String antes = estado(c);

        File inexistente = new File(System.getProperty("java.io.tmpdir"), "nao-existe-" + System.nanoTime() + ".dat");
        String msgAusente;
        try {
            c.carregar(inexistente);
            msgAusente = "CARREGOU (erro)";
        } catch (PersistenciaException e) {
            msgAusente = e.getMessage().startsWith("Arquivo não encontrado") ? "mensagem de arquivo não encontrado" : e.getMessage();
        }
        registrar("Problema no arquivo - arquivo ausente",
                "Carregar um arquivo que não existe",
                "mensagem de arquivo não encontrado", msgAusente);

        File invalido = File.createTempFile("cabenobolso-invalido", ".dat");
        try {
            Files.write(invalido.toPath(), "isto não é um planejamento".getBytes(StandardCharsets.UTF_8));
            String msgInvalido;
            try {
                c.carregar(invalido);
                msgInvalido = "CARREGOU (erro)";
            } catch (PersistenciaException e) {
                msgInvalido = e.getMessage().contains("inválido") ? "mensagem de arquivo inválido" : e.getMessage();
            }
            registrar("Problema no arquivo - conteúdo inválido",
                    "Carregar um arquivo de texto qualquer",
                    "mensagem de arquivo inválido", msgInvalido);

            Files.write(invalido.toPath(), new byte[0]);
            String msgVazio;
            try {
                c.carregar(invalido);
                msgVazio = "CARREGOU (erro)";
            } catch (PersistenciaException e) {
                msgVazio = "falha tratada com mensagem";
            }
            registrar("Problema no arquivo - arquivo vazio",
                    "Carregar um arquivo com 0 bytes", "falha tratada com mensagem", msgVazio);
        } finally {
            invalido.delete();
        }

        registrar("Problema no arquivo - planejamento em uso preservado",
                "Estado antes e depois das três falhas de carregamento",
                "estado idêntico", antes.equals(estado(c)) ? "estado idêntico" : "ESTADO ALTERADO");

        String falhaSalvar;
        try {
            c.salvar(new File("/pasta/que/nao/existe/arquivo.dat"));
            falhaSalvar = "SALVOU (erro)";
        } catch (PersistenciaException e) {
            falhaSalvar = e.getMessage().startsWith("Não foi possível salvar") ? "mensagem de falha ao salvar" : e.getMessage();
        }
        registrar("Problema no arquivo - destino inválido ao salvar",
                "Salvar em pasta inexistente", "mensagem de falha ao salvar", falhaSalvar);

        c.novoPlanejamento("Vazio");
        registrar("Problema no arquivo - iniciar planejamento vazio",
                "Novo planejamento após falhas",
                "0 participantes, 0 despesas, custo total=R$ 0,00",
                c.participantes().size() + " participantes, " + c.despesas().size() + " despesas, custo total="
                        + reais(c.resumo().getCustoTotal()));
    }

    /** Regras adicionais: exclusão de participante vinculado e edição de despesa. */
    private static void regrasComplementares() throws Exception {
        PlanejamentoController c = controladorComCaso();
        String bloqueio;
        try {
            c.excluirParticipante("2");
            bloqueio = "EXCLUIU (erro)";
        } catch (RegraNegocioException e) {
            bloqueio = "exclusão impedida";
        }
        registrar("Exclusão de participante com despesas vinculadas",
                "Excluir Bruno (código 2), que participa de despesas", "exclusão impedida", bloqueio);

        c.excluirDespesa(0);
        c.excluirDespesa(0);
        c.excluirDespesa(0);
        c.excluirDespesa(0);
        c.excluirParticipante("2");
        registrar("Exclusão de participante sem despesas",
                "Excluir todas as despesas e depois Bruno", "2 participantes restantes",
                c.participantes().size() + " participantes restantes");

        PlanejamentoController e = controladorComCaso();
        e.editarDespesa(2, TipoDespesa.POR_PESSOA, "Atividade recreativa", "30,00", Arrays.asList(1, 2, 3), false, 3);
        registrar("Edição de despesa recalcula totais",
                "Atividade recreativa vira 'Por pessoa' R$ 30,00 para os 3 participantes",
                "Ana=R$ 90,00; Bruno=R$ 90,00; Carla=R$ 90,00; custo total=R$ 270,00",
                totais(e.resumo()));
        String antes = estado(e);
        try {
            e.editarDespesa(2, TipoDespesa.FIXA, "Inválida", "-5", Arrays.asList(1), true, 0);
        } catch (RegraNegocioException ex) {
            // esperado
        }
        registrar("Edição inválida não altera a despesa",
                "Editar despesa com valor negativo", "estado idêntico",
                antes.equals(estado(e)) ? "estado idêntico" : "ESTADO ALTERADO");

        long somaIndividual = 0;
        for (Resumo.Linha l : e.resumo().getLinhas()) {
            somaIndividual += l.getTotal();
        }
        registrar("Invariante: soma dos totais individuais = custo total",
                "Caso editado acima", reais(e.resumo().getCustoTotal()), reais(somaIndividual));
    }

    /** Valores monetários digitados pelo usuário. */
    private static void dinheiro() throws Exception {
        String[] entradas = {"70", "70,5", "70,50", "70.50", "R$ 1.234,56", "1.234.567", "0", "0,01"};
        StringBuilder sb = new StringBuilder();
        for (String s : entradas) {
            sb.append(Dinheiro.parse(s)).append(' ');
        }
        registrar("Conversão de valores digitados (em centavos)",
                String.join(" | ", entradas), "7000 7050 7050 7050 123456 123456700 0 1 ", sb.toString());
    }

    /** Objetivo extra: simulador de alternativas. */
    private static void simulador() throws Exception {
        Planejamento p = CasoReferencia.criar();
        String antes = totais(p.resumo());
        ResultadoSimulacao r = cabenobolso.model.Simulador.simular(p);
        registrar("Simulador - caso de referência",
                "Transporte e almoço essenciais; atividade opcional (prioridade 3); sobremesa opcional (prioridade 1)",
                "viável; mantidas: Transporte, Almoço, Atividade recreativa; retirada: Sobremesa; "
                        + "Ana=R$ 65,00; Bruno=R$ 50,00; Carla=R$ 65,00; custo total=R$ 180,00; prioridade=3",
                (r.isViavel() ? "viável" : "sem solução") + "; mantidas: " + nomes(r.getMantidas())
                        + "; retirada: " + nomes(r.getRetiradas()) + "; " + totais(r.getNovoResumo())
                        + "; prioridade=" + r.getPrioridadeTotal());
        registrar("Simulador - original preservado",
                "Totais do planejamento antes e depois de simular", antes, totais(p.resumo()));
        registrar("Simulador - quantidade de despesas do original",
                "Número de despesas depois de simular", "4", String.valueOf(p.getDespesas().size()));

        p.alterarParticipante(1, "Ana", 4000);
        ResultadoSimulacao semSolucao = cabenobolso.model.Simulador.simular(p);
        registrar("Simulador - caso sem solução",
                "Orçamento de Ana reduzido para R$ 40,00 (essenciais custam R$ 50,00)",
                "sem solução; Ana excede em R$ 10,00",
                (semSolucao.isViavel() ? "viável" : "sem solução") + "; "
                        + (semSolucao.getExcessoEssenciais().size() == 1
                        ? "Ana excede em " + reais(semSolucao.getExcessoEssenciais().get(p.buscarParticipante(1)))
                        : semSolucao.getExcessoEssenciais().toString()));

        // Verificação contra força bruta independente (prioridades diferentes e empates).
        Planejamento q = new Planejamento("extra");
        q.cadastrarParticipante(1, "A", 5000);
        q.cadastrarParticipante(2, "B", 4000);
        q.incluirDespesa(TipoDespesa.FIXA, "Base", 2000, Arrays.asList(1, 2), true, 0);
        q.incluirDespesa(TipoDespesa.POR_PESSOA, "O1", 1500, Arrays.asList(1, 2), false, 5);
        q.incluirDespesa(TipoDespesa.POR_PESSOA, "O2", 1000, Arrays.asList(1, 2), false, 4);
        q.incluirDespesa(TipoDespesa.FIXA, "O3", 2000, Arrays.asList(1), false, 6);
        q.incluirDespesa(TipoDespesa.POR_PESSOA, "O4", 500, Arrays.asList(2), false, 2);
        ResultadoSimulacao rq = cabenobolso.model.Simulador.simular(q);
        long melhorBruto = forcaBruta(q);
        boolean cabe = true;
        for (Resumo.Linha l : rq.getNovoResumo().getLinhas()) {
            cabe &= l.isDentroDoOrcamento();
        }
        registrar("Simulador - ótimo confere com força bruta independente",
                "Cenário com 4 opcionais e prioridades 5, 4, 6, 2",
                "prioridade=" + melhorBruto + "; todos dentro do orçamento",
                "prioridade=" + rq.getPrioridadeTotal() + "; "
                        + (cabe ? "todos dentro do orçamento" : "alguém excede"));
    }

    private static String nomes(List<Despesa> despesas) {
        List<String> n = new ArrayList<>();
        for (Despesa d : despesas) {
            n.add(d.getDescricao());
        }
        return String.join(", ", n);
    }

    /** Implementação independente (usa subconjuntos por recursão) só para validar o simulador. */
    private static long forcaBruta(Planejamento p) {
        List<Despesa> essenciais = new ArrayList<>();
        List<Despesa> opcionais = new ArrayList<>();
        for (Despesa d : p.getDespesas()) {
            (d.isEssencial() ? essenciais : opcionais).add(d);
        }
        return melhor(p, essenciais, opcionais, 0, 0);
    }

    private static long melhor(Planejamento p, List<Despesa> mantidas, List<Despesa> opcionais, int i, long prioridade) {
        if (i == opcionais.size()) {
            Resumo r = Resumo.calcular(p.getParticipantes(), mantidas);
            for (Resumo.Linha l : r.getLinhas()) {
                if (!l.isDentroDoOrcamento()) {
                    return -1;
                }
            }
            return prioridade;
        }
        long sem = melhor(p, mantidas, opcionais, i + 1, prioridade);
        List<Despesa> com = new ArrayList<>(mantidas);
        com.add(opcionais.get(i));
        long comOpcional = melhor(p, com, opcionais, i + 1, prioridade + opcionais.get(i).getPrioridade());
        return Math.max(sem, comOpcional);
    }

    /** Cenário próprio da equipe ("Dia de praia"): rateio, situação e centavos. */
    private static void cenarioEquipe() throws Exception {
        Planejamento p = CenarioEquipe.criar();
        Resumo r = p.resumo();
        registrar("Cenário da equipe - totais",
                "Evando(155), Marcelo(155), Isac(150), D'jonathan(130); Van fixa 250 (todos); Almoço p/pessoa 35 (todos); "
                        + "Buggy p/pessoa 40 (1,3); Barraca fixa 100 (1,2,3); Água de coco p/pessoa 8,50 (todos); Fotos fixa 60 (1,2,4)",
                "Evando=R$ 199,34; Marcelo=R$ 159,33; Isac=R$ 179,33; D'jonathan=R$ 126,00; custo total=R$ 664,00",
                totais(r));

        StringBuilder situacao = new StringBuilder();
        for (Resumo.Linha l : r.getLinhas()) {
            situacao.append(l.getNome()).append(l.isDentroDoOrcamento() ? " dentro" : " excede " + reais(l.getExcesso()))
                    .append("; ");
        }
        registrar("Cenário da equipe - situação no orçamento",
                "Mesma entrada",
                "Evando excede R$ 44,34; Marcelo excede R$ 4,33; Isac excede R$ 29,33; D'jonathan dentro; ",
                situacao.toString());

        Despesa barraca = p.getDespesas().get(3);
        StringBuilder sb = new StringBuilder();
        for (Participante pessoa : p.getParticipantes()) {
            sb.append(pessoa.getNome()).append(' ').append(reais(barraca.contribuicaoDe(pessoa))).append("; ");
        }
        registrar("Cenário da equipe - centavos e participação seletiva",
                "Barraca fixa de R$ 100,00 só para os códigos 1, 2 e 3",
                "Evando R$ 33,34; Marcelo R$ 33,33; Isac R$ 33,33; D'jonathan R$ 0,00; ",
                sb.toString());

        String antes = totais(p.resumo());
        ResultadoSimulacao s = cabenobolso.model.Simulador.simular(p);
        registrar("Cenário da equipe - simulador",
                "Van e almoço essenciais; buggy (5), barraca (3), água de coco (1) e fotos (2) opcionais",
                "viável; mantidas: Van, Almoço, Passeio de buggy, Água de coco; retiradas: Barraca e cadeiras, Fotos profissionais; "
                        + "Evando=R$ 146,00; Marcelo=R$ 106,00; Isac=R$ 146,00; D'jonathan=R$ 106,00; "
                        + "custo total=R$ 504,00; prioridade=6",
                (s.isViavel() ? "viável" : "sem solução") + "; mantidas: " + nomes(s.getMantidas())
                        + "; retiradas: " + nomes(s.getRetiradas()) + "; " + totais(s.getNovoResumo())
                        + "; prioridade=" + s.getPrioridadeTotal());
        registrar("Cenário da equipe - simulador confere com força bruta independente",
                "Mesma entrada", "prioridade=" + forcaBruta(p), "prioridade=" + s.getPrioridadeTotal());
        registrar("Cenário da equipe - original preservado após simular",
                "Totais antes e depois da simulação", antes, totais(p.resumo()));

        // Limite exato: total igual ao orçamento conta como dentro (Isac fica com R$ 146,00).
        Planejamento exato = CenarioEquipe.criar();
        exato.alterarParticipante(3, "Isac", 14600);
        registrar("Cenário da equipe - limite exato é aceito",
                "Orçamento de Isac reduzido para R$ 146,00 (igual ao total da melhor combinação)",
                "prioridade=6", "prioridade=" + cabenobolso.model.Simulador.simular(exato).getPrioridadeTotal());

        // Um centavo abaixo do limite: muda a melhor combinação (e continua conferindo com a força bruta).
        Planejamento abaixo = CenarioEquipe.criar();
        abaixo.alterarParticipante(3, "Isac", 14599);
        ResultadoSimulacao sa = cabenobolso.model.Simulador.simular(abaixo);
        registrar("Cenário da equipe - um centavo abaixo do limite",
                "Orçamento de Isac reduzido para R$ 145,99",
                "prioridade=5 (força bruta: " + forcaBruta(abaixo) + "); todos dentro do orçamento",
                "prioridade=" + sa.getPrioridadeTotal() + " (força bruta: " + forcaBruta(abaixo) + "); "
                        + (todosDentro(sa.getNovoResumo()) ? "todos dentro do orçamento" : "alguém excede"));

        // Sem solução: as essenciais (R$ 97,50) não cabem no orçamento de D'jonathan.
        Planejamento sem = CenarioEquipe.criar();
        sem.alterarParticipante(4, "D'jonathan", 9000);
        ResultadoSimulacao ss = cabenobolso.model.Simulador.simular(sem);
        registrar("Cenário da equipe - sem solução",
                "Orçamento de D'jonathan reduzido para R$ 90,00 (essenciais custam R$ 97,50)",
                "sem solução; D'jonathan excede em R$ 7,50",
                (ss.isViavel() ? "viável" : "sem solução") + "; D'jonathan excede em "
                        + reais(ss.getExcessoEssenciais().get(sem.buscarParticipante(4))));
    }

    private static boolean todosDentro(Resumo r) {
        for (Resumo.Linha l : r.getLinhas()) {
            if (!l.isDentroDoOrcamento()) {
                return false;
            }
        }
        return true;
    }

    /** Os arquivos de exemplo versionados em data/ precisam carregar e reproduzir os resultados. */
    private static void arquivosExemplo() throws Exception {
        String[][] arquivos = {{"data/caso-referencia.dat", "ref"}, {"data/cenario-equipe.dat", "equipe"}};
        for (String[] a : arquivos) {
            Planejamento esperado = a[1].equals("ref") ? CasoReferencia.criar() : CenarioEquipe.criar();
            String obtido;
            try {
                PlanejamentoController c = new PlanejamentoController(ArquivoPlanejamentoRepository::new);
                c.carregar(new File(a[0]));
                obtido = totais(c.resumo());
            } catch (PersistenciaException e) {
                obtido = "FALHA: " + e.getMessage() + " (execute a partir da raiz do projeto)";
            }
            registrar("Arquivo de exemplo " + a[0],
                    "Carregar o arquivo versionado no repositório e conferir os totais",
                    totais(esperado.resumo()), obtido);
        }
    }

    // ------------------------------------------------------------------ execução

    public static void main(String[] args) throws IOException {        rodar("Caso de referência", TestesRunner::casoReferencia);
        rodar("Centavos", TestesRunner::centavos);
        rodar("Entradas inválidas", TestesRunner::entradasInvalidas);
        rodar("Alteração de orçamento", TestesRunner::alteracaoOrcamento);
        rodar("Exclusão de despesa", TestesRunner::exclusaoDespesa);
        rodar("Salvar e reiniciar", TestesRunner::salvarEReiniciar);
        rodar("Problemas de arquivo", TestesRunner::problemasArquivo);
        rodar("Regras complementares", TestesRunner::regrasComplementares);
        rodar("Dinheiro", TestesRunner::dinheiro);
        rodar("Simulador", TestesRunner::simulador);
        rodar("Cenário da equipe", TestesRunner::cenarioEquipe);
        rodar("Arquivos de exemplo", TestesRunner::arquivosExemplo);

        StringBuilder md = new StringBuilder();
        md.append("# Evidências dos testes\n\n");
        md.append("| # | Cenário | Entrada | Resultado esperado | Resultado obtido | Situação |\n");
        md.append("|---|---------|---------|--------------------|------------------|----------|\n");
        int falhas = 0;
        int numero = 1;
        for (Resultado r : RESULTADOS) {
            md.append("| ").append(numero++).append(" | ").append(cel(r.cenario)).append(" | ").append(cel(r.entrada))
                    .append(" | ").append(cel(r.esperado)).append(" | ").append(cel(r.obtido)).append(" | ")
                    .append(r.ok ? "OK" : "FALHOU").append(" |\n");
            if (!r.ok) {
                falhas++;
            }
        }
        md.append("\n**Total: ").append(RESULTADOS.size()).append(" verificações, ")
                .append(RESULTADOS.size() - falhas).append(" aprovadas, ").append(falhas).append(" falhas.**\n");
        java.io.PrintStream console = new java.io.PrintStream(System.out, true, "UTF-8");
        console.print(md);
        console.flush();
        if (args.length > 0) {
            File saida = new File(args[0]);
            File pasta = saida.getAbsoluteFile().getParentFile();
            if (pasta != null) {
                pasta.mkdirs();
            }
            Files.write(saida.toPath(), md.toString().getBytes(StandardCharsets.UTF_8));
        }
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static String cel(String s) {
        return s.replace("|", "\\|").replace("\n", " ");
    }
}
