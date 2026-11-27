package cabenobolso.persistence;

import cabenobolso.model.Planejamento;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persistência por serialização de objetos em um arquivo local.
 * Como o grafo (planejamento, participantes e despesas) é serializado em um
 * único fluxo, as referências compartilhadas entre despesas e participantes
 * são preservadas na leitura, sem duplicar registros.
 */
public class ArquivoPlanejamentoRepository implements RepositorioPlanejamento {

    private final File arquivo;

    public ArquivoPlanejamentoRepository(File arquivo) {
        this.arquivo = arquivo;
    }

    @Override
    public void salvar(Planejamento planejamento) throws PersistenciaException {
        if (planejamento == null) {
            throw new PersistenciaException("Não há planejamento para salvar.", null);
        }
        Path destino = arquivo.getAbsoluteFile().toPath();
        Path temporario = null;
        try {
            // Grava num arquivo temporário e só então substitui o destino:
            // uma falha no meio da escrita não destrói o arquivo anterior.
            temporario = Files.createTempFile(destino.getParent(), "cabenobolso-", ".tmp");
            try (ObjectOutputStream saida = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporario)))) {
                saida.writeObject(planejamento);
            }
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            if (temporario != null) {
                try {
                    Files.deleteIfExists(temporario);
                } catch (IOException ignorada) {
                    // nada a fazer: o arquivo temporário será ignorado
                }
            }
            throw new PersistenciaException("Não foi possível salvar o arquivo \"" + arquivo.getPath()
                    + "\": " + e.getMessage(), e);
        }
    }

    @Override
    public Planejamento carregar() throws PersistenciaException {
        if (!arquivo.exists()) {
            throw new PersistenciaException("Arquivo não encontrado: " + arquivo.getPath(), null);
        }
        if (!arquivo.isFile()) {
            throw new PersistenciaException("O caminho informado não é um arquivo: " + arquivo.getPath(), null);
        }
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(arquivo)))) {
            Object lido = entrada.readObject();
            if (!(lido instanceof Planejamento)) {
                throw new PersistenciaException("O arquivo não contém um planejamento válido.", null);
            }
            return (Planejamento) lido;
        } catch (PersistenciaException e) {
            throw e;
        } catch (ObjectStreamException | EOFException e) {
            throw new PersistenciaException("O arquivo \"" + arquivo.getName()
                    + "\" é inválido, está corrompido ou foi gerado por outra versão do programa.", e);
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenciaException("Não foi possível ler o arquivo \"" + arquivo.getPath()
                    + "\": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new PersistenciaException("O arquivo \"" + arquivo.getName() + "\" não pôde ser interpretado.", e);
        }
    }
}
