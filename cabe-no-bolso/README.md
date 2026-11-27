# Cabe no bolso – planejador de despesas compartilhadas

Aplicação Java (Swing) para organizar um passeio ou atividade em grupo: cadastra participantes e
despesas (fixas ou por pessoa), calcula quanto cada um paga em centavos exatos, mostra se cada
participante respeita seu orçamento e salva/recupera o planejamento em arquivo. Inclui o **objetivo
extra** (simulador de alternativas).

Trabalho final de Programação Orientada a Objetos (EAD Unifor) – **Grupo 10**.
Integrantes: Marcelo de Oliveira Saraiva (2641741), **[PREENCHER: demais nomes e matrículas]**.

## Requisitos

| Item | Versão |
|------|--------|
| JDK  | **11 ou superior** (desenvolvido e testado com JDK 21; o código também compila com `--release 8/11/17`) |
| Dependências externas | **nenhuma** (somente a biblioteca padrão do Java: Swing, java.io, java.util) |
| Sistema | Windows, Linux ou macOS com ambiente gráfico para a interface |

Não há instalação: basta compilar e executar.

## Organização das pastas

```
cabe-no-bolso/
├── src/main/java/cabenobolso/
│   ├── Main.java                  ponto de entrada
│   ├── model/                     MODELO: Planejamento, Participante, Despesa (abstrata),
│   │                              DespesaFixa, DespesaPorPessoa, TipoDespesa, Resumo,
│   │                              Simulador, ResultadoSimulacao, RegraNegocioException
│   ├── controller/                CONTROLADOR: PlanejamentoController
│   ├── view/                      VISÃO (Swing): JanelaPrincipal e painéis das abas
│   ├── persistence/               RepositorioPlanejamento (interface),
│   │                              ArquivoPlanejamentoRepository (serialização), PersistenciaException
│   └── util/                      Dinheiro, CasoReferencia, CenarioEquipe, GeradorExemplo
├── src/test/java/cabenobolso/     TestesRunner (testes) e CapturaTelas (capturas da interface)
├── data/                          arquivos de exemplo (.dat) compatíveis com esta versão
├── docs/                          diagramas UML, evidências dos testes, capturas e relatório
└── scripts/                       compilar, executar, testar
```

## Compilar e executar

Na raiz do projeto.

**Linux / macOS**

```bash
./scripts/compilar.sh      # gera as classes em out/
./scripts/executar.sh      # abre a aplicação
```

Equivalente manual:

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out cabenobolso.Main
```

**Windows (PowerShell)** — *não testado no Windows pela autoria dos scripts; os comandos são os mesmos do Java*

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out cabenobolso.Main
```

## Como usar a aplicação

A janela tem quatro abas e o menu **Arquivo**.

1. **Participantes**: informe código (inteiro positivo, único), nome e orçamento (ex.: `70,00`) e
   clique em *Cadastrar*. Selecione uma linha para *Alterar nome/orçamento* ou *Excluir*
   (a exclusão é impedida se o participante ainda participa de alguma despesa).
2. **Despesas**: informe descrição, valor, tipo (*Fixa*: o valor é dividido entre os selecionados;
   *Por pessoa*: cada selecionado paga o valor), classificação (*Essencial* ou *Opcional* com
   prioridade, usada só pelo simulador) e marque os participantes envolvidos. Selecione uma linha
   para editar ou excluir.
3. **Resumo**: despesas, custo total, quanto cada participante paga em cada despesa, total
   individual e situação (*Dentro do orçamento* em verde / *Excede em R$ x* em vermelho). Total igual
   ao orçamento conta como dentro.
4. **Simulador (extra)**: *Simular alternativas* sugere quais despesas opcionais manter.
   O planejamento original não é alterado.

Menu **Arquivo**: novo planejamento, carregar o caso de referência, carregar o cenário da equipe,
**Abrir...** e **Salvar...** (serialização de objetos). Um planejamento acima do orçamento pode ser
salvo normalmente para revisão. Se um arquivo não puder ser aberto, a aplicação mostra a mensagem e
**mantém o planejamento em uso**.

### Arquivos de exemplo

* `data/caso-referencia.dat` – caso da seção 4 do enunciado (Ana, Bruno e Carla).
* `data/cenario-equipe.dat` – cenário fictício da equipe ("Dia de praia", 4 participantes, 6 despesas).

Use *Arquivo → Abrir...* e selecione um deles. Se alguma classe serializável for alterada, gere os
arquivos de novo (os `.dat` antigos deixam de ser compatíveis):

```bash
java -cp out cabenobolso.util.GeradorExemplo data
```

### Roteiro para cadastrar o caso de referência manualmente

Participantes (código, nome, orçamento): `1 Ana 70,00` · `2 Bruno 60,00` · `3 Carla 70,00`.

| Descrição | Tipo | Valor | Participantes | Classificação |
|-----------|------|-------|---------------|---------------|
| Transporte | Fixa | 90,00 | Ana, Bruno, Carla | Essencial |
| Almoço | Por pessoa | 20,00 | Ana, Bruno, Carla | Essencial |
| Atividade recreativa | Fixa | 30,00 | Ana, Carla | Opcional, prioridade 3 |
| Sobremesa | Por pessoa | 10,00 | Ana, Bruno, Carla | Opcional, prioridade 1 |

Resultado esperado na aba *Resumo*: custo total **R$ 210,00**; Ana R$ 75,00 e Carla R$ 75,00
(excedem em R$ 5,00 cada); Bruno R$ 60,00 (dentro, igual ao orçamento).
Na aba *Simulador*: retira a sobremesa; Ana e Carla R$ 65,00, Bruno R$ 50,00, total R$ 180,00.
Com o orçamento de Ana em R$ 40,00: sem solução (essenciais custam R$ 50,00).

## Como reproduzir os testes

```bash
./scripts/testes.sh
```

Compila, executa `cabenobolso.TestesRunner` (a partir da raiz, pois lê os arquivos de `data/`) e
grava `docs/evidencias-testes.md` com **entrada, resultado esperado, resultado obtido e situação**
de cada verificação (atualmente 43). O programa termina com código 0 se todas passarem.
Cobre: caso de referência, centavos no rateio, entradas inválidas (sem alterar dados),
alteração de orçamento, exclusão de despesa, salvar e reiniciar, problemas de arquivo,
edição/exclusão com vínculos, conversão de valores, simulador (caso de referência, sem solução,
limite exato, um centavo abaixo do limite, comparação com busca independente) e o cenário da equipe.

As capturas de tela (pasta `docs/capturas/`) são geradas pela própria interface real:

```bash
./scripts/capturas.sh      # usa xvfb-run quando não há monitor
```

## Modelagem

* `docs/diagrama-modelo.png` e `docs/diagrama-mvc.png` – figuras do relatório (fontes `.dot` e `.svg`
  ao lado); `docs/diagrama-classes.png` – diagrama completo com todos os atributos e operações.
* Justificativas: o relatório (`docs/relatorio/`) descreve as relações (composição de participantes e
  despesas em `Planejamento`; associação `Despesa → Participante` 1..*), herança e polimorfismo
  (`Despesa.ratear()` abstrato), encapsulamento e a interface de persistência.
* Relatório técnico: fonte editável em `docs/relatorio/relatorio.html` (instruções para gerar o PDF em
  `docs/relatorio/LEIA-ME.md`).
* Para regenerar as figuras: `dot -Tpng docs/diagrama-modelo.dot -o docs/diagrama-modelo.png` (Graphviz).

## Decisões principais

* **Dinheiro em centavos (`long`)**; nenhum `double`. No rateio fixo, os centavos que sobram vão,
  um a um, para os selecionados em ordem crescente de código.
* **Validar antes de alterar**: toda operação do modelo valida primeiro e só então muda o estado;
  uma entrada inválida lança `RegraNegocioException` com mensagem clara e nada é modificado.
* **Resumo sempre recalculado** a partir das despesas (nunca guardado), então editar, excluir ou
  alterar orçamentos mantém os totais consistentes.
* **Persistência por serialização** de todo o grafo em um único fluxo (`ObjectOutputStream`), o que
  preserva os vínculos (as despesas apontam para os mesmos objetos `Participante`) sem duplicar
  registros. A gravação usa arquivo temporário e só substitui o destino ao final.
* **Simulador** (objetivo extra): busca exaustiva sobre os 2^k subconjuntos de opcionais
  (k ≤ 10 ⇒ no máximo 1024 combinações), descrita em `Simulador.java`.

## Limitações conhecidas

Um planejamento por vez; sem desfazer/refazer; valores em uma só moeda; divisão sempre igualitária
entre os selecionados; arquivo binário (não legível por humanos e dependente da versão das classes);
simulador limitado a 10 despesas opcionais.

## Uso de IA

O código-base e parte da documentação deste repositório foram produzidos com apoio de uma
ferramenta de IA generativa (Claude, da Anthropic). **[PREENCHER pela equipe: o que foi revisado,
executado e compreendido por cada integrante e quais decisões foram da equipe.]**
