##  O que precisamos fazer

### <ins>Entrada e Saída

#### Movimentação dos processos para fila de bloqueados automaticamente

- quando um processo solicita acesso a um dispositivo de I/O, devem ocorrer as seguintes ações: 
1. O processo deve ser movido para bloqueado. ✅
2. Outro processo deve ser escalonado. ✅
3. O pedido de I/O deve ser despachado para fila do dispositivo e a operação realizada no mesmo.

As ações devem ser realizadas concorrentemente, com *multithreading*

#### Criar dispositivo de I/O

#### Interrupção de finalização de I/O

- Devemos criar uma variável na CPU para que o dispositivo de I/O sinalize a interrupção.
- O processo que estava bloqueado deve ser movido para a fila de prontos para ser escalonado novamente.
- O processo que foi interrompido também volta para a fila de prontos.
- As operações de I/O são realizadas *sem envolver a CPU*


### <ins>Memória Virtual

#### Carregar somente uma página na memória

- apenas a primeira página de um programa deve ser carregada na memória (para acontecer page-faults)


#### Criação de um disco
- Disco contém os programas armazenados e *os quadros de memória salvos*, com seus estados de execução!

*Funções do Disco:*
1. trazer páginas específicas de programas específicos para um quadro da Mem
2. salvar páginas vitimadas no disco, junto dos estados de execução
3. trazer páginas, anteriormente vitimadas de volta para a memória, em quadros disponíveis

*Esquema de Paginação: A página pode...*
- nunca ter sido carregada em memória -> tem seu conteúdo original (sem estado de execução)
- já ter sido carregada anteriormente na Mem -> seu conteúdo estará em um quadro da Mem OU no disco (com o estado de execução)

Essa informação deve estar na *tabela de páginas.*
> podemos implementar as ações do disco parecido como fizemos com o MemoryManager (aloca e libera quadros)


#### Vitimização de Páginas Gerente de Memória

- quando ocorre um page-fault, o Gerente de Memória deve *procurar um quadro livre* (é requisitado uma página a mais para o GM)
	+ [*]deve-se adicionar o mapeamento página/quadro na tabela de páginas do processo
	+ encaminha pedido pra trazer a página do processo que estava no disco (memória virtual) à memória principal para fazer paginação
	+ processo executando vai pra fila de bloqueados
	+ escalona outro processo
- *Se não encontrar um quadro livre*, ele vitimiza uma página que está ocupando um quadro (algoritmo do relógio)
	- página vitimada é salva em disco (podendo ser trazida novamente)
	- processo executando vai pra estado bloqueado
	- escalona outro processo

*Interrupções novas:*
- Fim de salvamento de página em disco:
	- libera o quadro (página não está mais no quadro da Mem, e sim no disco)
	- passa o quadro da Mem para a página do processo que requisitou
	- continua em [*]
- Fim do carregamento da página ao quadro da Mem (que estava no disco)
	+ passa o processo da fila de bloqueados para pronto (fim do tratamento de page-fault)

#### Adicionar lista de interrupções
- várias interrupções podem ocorrer ao mesmo tempo
- precisamos armazenar as interrupções em uma lista ou fila para tratamento

#### Visualização dos quadros dos processos
- deve ser possível visualizar quais quadros utilizados por um processo e ver a mudança deles durante a execução!
