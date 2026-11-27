package cabenobolso.util;

import cabenobolso.model.Planejamento;
import cabenobolso.persistence.ArquivoPlanejamentoRepository;
import cabenobolso.persistence.PersistenciaException;

import java.io.File;

/**
 * Gera os arquivos de exemplo (caso de referência e cenário da equipe),
 * compatíveis com a versão atual das classes serializáveis.
 * Uso: java -cp out cabenobolso.util.GeradorExemplo [pasta-de-destino]
 * Se alguma classe de dados mudar, execute de novo para atualizar os arquivos.
 */
public final class GeradorExemplo {

    private GeradorExemplo() {
    }

    public static void main(String[] args) throws PersistenciaException {
        File pasta = new File(args.length > 0 ? args[0] : "data");
        pasta.mkdirs();
        gravar(new File(pasta, "caso-referencia.dat"), CasoReferencia.criar());
        gravar(new File(pasta, "cenario-equipe.dat"), CenarioEquipe.criar());
    }

    private static void gravar(File destino, Planejamento planejamento) throws PersistenciaException {
        new ArquivoPlanejamentoRepository(destino).salvar(planejamento);
        System.out.println("Arquivo de exemplo gerado em " + destino.getPath());
    }
}
