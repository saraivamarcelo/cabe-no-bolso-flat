# Relatório técnico

* `relatorio.html` – **fonte editável** do relatório (uma coluna, A4, margens ABNT). As marcas em
  **amarelo** (`<mark class="pend">`) são os campos que só a equipe pode preencher: nomes completos e
  matrículas, instituição, link do GitHub, contribuições de cada integrante e o parágrafo sobre uso de IA.
  Depois de preencher, apague as tags `<mark class="pend">` e `</mark>` (o texto fica).
* `relatorio-rascunho.pdf` – PDF gerado **antes** do preenchimento (9 páginas). Não entregue este arquivo.

## Como gerar o PDF final (máx. 10 páginas)

1. Abra `relatorio.html` no Google Chrome (as imagens vêm de `../capturas` e `../diagrama-*.png`,
   então mantenha a estrutura de pastas).
2. `Ctrl+P` → Destino: **Salvar como PDF** → Margens: **Padrão** → Tamanho do papel: A4.
3. Confira que o total é de **10 páginas ou menos** e que não sobrou nenhum campo amarelo.
4. Envie o mesmo PDF no AVA (cada integrante).

Se acrescentar integrantes ou texto e passar de 10 páginas, reduza o tamanho das figuras
(atributo `style="width: ...cm"` em cada `<img>`) ou encurte parágrafos.
