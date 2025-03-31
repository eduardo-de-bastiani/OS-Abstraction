// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Estrutura deste código:
//    Todo código está dentro da classe *Sistema*
//    Dentro de Sistema, encontra-se acima a definição de HW:
//           Memory,  Word, 
//           CPU tem Opcodes (codigos de operacoes suportadas na cpu),
//               e Interrupcoes possíveis, define o que executa para cada instrucao
//           VM -  a máquina virtual é uma instanciação de CPU e Memória
//    Depois as definições de SW:
//           no momento são esqueletos (so estrutura) para
//					InterruptHandling    e
//					SysCallHandling 
//    A seguir temos utilitários para usar o sistema
//           carga, início de execução e dump de memória
//    Por último os programas existentes, que podem ser copiados em memória.
//           Isto representa programas armazenados.
//    Veja o main.  Ele instancia o Sistema com os elementos mencionados acima.
//           em seguida solicita a execução de algum programa com  loadAndExec

import java.util.*;

public class Sistema {

	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW
	// ----------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A - definicoes de palavra de memoria,
	// memória ----------------------

	public class Memory {
		public Word[] pos; // pos[i] é a posição i da memória. cada posição é uma palavra.

		public Memory(int size) {
			pos = new Word[size];
			for (int i = 0; i < pos.length; i++) {
				pos[i] = new Word(Opcode.___, -1, -1, -1);
			}
			; // cada posicao da memoria inicializada
		}
	}

	public class Word { // cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; //
		public int ra; // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int rb; // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p; // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _ra, int _rb, int _p) { // vide definição da VM - colunas vermelhas da tabela
			opc = _opc;
			ra = _ra;
			rb = _rb;
			p = _p;
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- C P U - definicoes da CPU
	// -----------------------------------------------------

	public enum Opcode {
		DATA, ___, // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE, // desvios
		JMPIM, JMPIGM, JMPILM, JMPIEM,
		JMPIGK, JMPILK, JMPIEK, JMPIGT,
		ADDI, SUBI, ADD, SUB, MULT, // matematicos
		LDI, LDD, STD, LDX, STX, MOVE, // movimentacao
		SYSCALL, STOP // chamada de sistema e parada
	}

	public enum Interrupts { // possiveis interrupcoes que esta CPU gera
		noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP;
	}

	public class CPU {
		private int maxInt; // valores maximo e minimo para inteiros nesta cpu
		private int minInt;
		// CONTEXTO da CPU ...
		private int pc; // ... composto de program counter,
		private Word ir; // instruction register,
		private int[] reg; // registradores da CPU
		private Interrupts irpt; // durante instrucao, interrupcao pode ser sinalizada
		// FIM CONTEXTO DA CPU: tudo que precisa sobre o estado de um processo para
		// executa-lo
		// nas proximas versoes isto pode modificar

		private Word[] m; // m é o array de memória "física", CPU tem uma ref a m para acessar

		private InterruptHandling ih; // significa desvio para rotinas de tratamento de Int - se int ligada, desvia
		private SysCallHandling sysCall; // significa desvio para tratamento de chamadas de sistema

		private boolean cpuStop; // flag para parar CPU - caso de interrupcao que acaba o processo, ou chamada
									// stop -
									// nesta versao acaba o sistema no fim do prog

		// auxilio aa depuração
		private boolean debug; // se true entao mostra cada instrucao em execucao
		private Utilities u; // para debug (dump)

		public CPU(Memory _mem, boolean _debug) { // ref a MEMORIA passada na criacao da CPU
			maxInt = 32767; // capacidade de representacao modelada
			minInt = -32767; // se exceder deve gerar interrupcao de overflow
			m = _mem.pos; // usa o atributo 'm' para acessar a memoria, só para ficar mais pratico
			reg = new int[10]; // aloca o espaço dos registradores - regs 8 e 9 usados somente para IO

			debug = _debug; // se true, print da instrucao em execucao

		}

		// TODO: implementar TLB (Translation Lookaside Buffer)
		// local para armazenar as traduções dos endereços virtuais para físicos

		public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
			ih = _ih; // aponta para rotinas de tratamento de int
			sysCall = _sysCall; // aponta para rotinas de tratamento de chamadas de sistema
		}

		public void setUtilities(Utilities _u) {
			u = _u; // aponta para rotinas utilitárias - fazer dump da memória na tela
		}

		// verificação de enderecamento
		private boolean legal(int e) { // todo acesso a memoria tem que ser verificado se é válido -
										// aqui no caso se o endereco é um endereco valido em toda memoria
			if (e >= 0 && e < m.length) {
				return true;
			} else {
				irpt = Interrupts.intEnderecoInvalido; // se nao for liga interrupcao no meio da exec da instrucao
				return false;
			}
		}

		private boolean testOverflow(int v) { // toda operacao matematica deve avaliar se ocorre overflow
			if ((v < minInt) || (v > maxInt)) {
				irpt = Interrupts.intOverflow; // se houver liga interrupcao no meio da exec da instrucao
				return false;
			}
			;
			return true;
		}

		public void setContext(int _pc) { // usado para setar o contexto da cpu para rodar um processo
											// [ nesta versao é somente colocar o PC na posicao 0 ]
			pc = _pc; // pc cfe endereco logico
			irpt = Interrupts.noInterrupt; // reset da interrupcao registrada
		}

		public void run() { // execucao da CPU supoe que o contexto da CPU, vide acima,
							// esta devidamente setado
			cpuStop = false;
			while (!cpuStop) { // ciclo de instrucoes. acaba cfe resultado da exec da instrucao, veja cada
								// caso.

				// --------------------------------------------------------------------------------------------------
				// FASE DE FETCH
				if (legal(pc)) { // pc valido
					ir = m[pc]; // <<<<<<<<<<<< AQUI faz FETCH - busca posicao da memoria apontada por pc,
								// guarda em ir
								// resto é dump de debug
					if (debug) {
						System.out.print("                                              regs: ");
						for (int i = 0; i < 10; i++) {
							System.out.print(" r[" + i + "]:" + reg[i]);
						}
						;
						System.out.println();
					}
					if (debug) {
						System.out.print("                      pc: " + pc + "       exec: ");
						u.dump(ir);
					}

					// --------------------------------------------------------------------------------------------------
					// FASE DE EXECUCAO DA INSTRUCAO CARREGADA NO ir
					switch (ir.opc) { // conforme o opcode (código de operação) executa

						// Instrucoes de Busca e Armazenamento em Memoria
						case LDI: // Rd ← k veja a tabela de instrucoes do HW simulado para entender a semantica
									// da instrucao
							reg[ir.ra] = ir.p;
							pc++;
							break;
						case LDD: // Rd <- [A]
							if (legal(ir.p)) {
								reg[ir.ra] = m[ir.p].p;
								pc++;
							}
							break;
						case LDX: // RD <- [RS] // NOVA
							if (legal(reg[ir.rb])) {
								reg[ir.ra] = m[reg[ir.rb]].p;
								pc++;
							}
							break;
						case STD: // [A] ← Rs
							if (legal(ir.p)) {
								m[ir.p].opc = Opcode.DATA;
								m[ir.p].p = reg[ir.ra];
								pc++;
								if (debug) {
									System.out.print("                                  ");
									u.dump(ir.p, ir.p + 1);
								}
							}
							break;
						case STX: // [Rd] ←Rs
							if (legal(reg[ir.ra])) {
								m[reg[ir.ra]].opc = Opcode.DATA;
								m[reg[ir.ra]].p = reg[ir.rb];
								pc++;
							}
							;
							break;
						case MOVE: // RD <- RS
							reg[ir.ra] = reg[ir.rb];
							pc++;
							break;
						// Instrucoes Aritmeticas
						case ADD: // Rd ← Rd + Rs
							reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
							testOverflow(reg[ir.ra]);
							pc++;
							break;
						case ADDI: // Rd ← Rd + k
							reg[ir.ra] = reg[ir.ra] + ir.p;
							testOverflow(reg[ir.ra]);
							pc++;
							break;
						case SUB: // Rd ← Rd - Rs
							reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
							testOverflow(reg[ir.ra]);
							pc++;
							break;
						case SUBI: // RD <- RD - k // NOVA
							reg[ir.ra] = reg[ir.ra] - ir.p;
							testOverflow(reg[ir.ra]);
							pc++;
							break;
						case MULT: // Rd <- Rd * Rs
							reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
							testOverflow(reg[ir.ra]);
							pc++;
							break;

						// Instrucoes JUMP
						case JMP: // PC <- k
							pc = ir.p;
							break;
						case JMPIM: // PC <- [A]
							pc = m[ir.p].p;
							break;
						case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
							if (reg[ir.rb] > 0) {
								pc = reg[ir.ra];
							} else {
								pc++;
							}
							break;
						case JMPIGK: // If RC > 0 then PC <- k else PC++
							if (reg[ir.rb] > 0) {
								pc = ir.p;
							} else {
								pc++;
							}
							break;
						case JMPILK: // If RC < 0 then PC <- k else PC++
							if (reg[ir.rb] < 0) {
								pc = ir.p;
							} else {
								pc++;
							}
							break;
						case JMPIEK: // If RC = 0 then PC <- k else PC++
							if (reg[ir.rb] == 0) {
								pc = ir.p;
							} else {
								pc++;
							}
							break;
						case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
							if (reg[ir.rb] < 0) {
								pc = reg[ir.ra];
							} else {
								pc++;
							}
							break;
						case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
							if (reg[ir.rb] == 0) {
								pc = reg[ir.ra];
							} else {
								pc++;
							}
							break;
						case JMPIGM: // If RC > 0 then PC <- [A] else PC++
							if (legal(ir.p)) {
								if (reg[ir.rb] > 0) {
									pc = m[ir.p].p;
								} else {
									pc++;
								}
							}
							break;
						case JMPILM: // If RC < 0 then PC <- k else PC++
							if (reg[ir.rb] < 0) {
								pc = m[ir.p].p;
							} else {
								pc++;
							}
							break;
						case JMPIEM: // If RC = 0 then PC <- k else PC++
							if (reg[ir.rb] == 0) {
								pc = m[ir.p].p;
							} else {
								pc++;
							}
							break;
						case JMPIGT: // If RS>RC then PC <- k else PC++
							if (reg[ir.ra] > reg[ir.rb]) {
								pc = ir.p;
							} else {
								pc++;
							}
							break;

						case DATA: // pc está sobre área supostamente de dados
							irpt = Interrupts.intInstrucaoInvalida;
							break;

						// Chamadas de sistema
						case SYSCALL:
							sysCall.handle(); // <<<<< aqui desvia para rotina de chamada de sistema, no momento so
												// temos IO
							pc++;
							break;

						case STOP: // por enquanto, para execucao
							sysCall.stop();
							cpuStop = true;
							break;

						// Inexistente
						default:
							irpt = Interrupts.intInstrucaoInvalida;
							break;
					}
				}
				// --------------------------------------------------------------------------------------------------
				// VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
				if (irpt != Interrupts.noInterrupt) { // existe interrupção
					ih.handle(irpt); // desvia para rotina de tratamento - esta rotina é do SO
					cpuStop = true; // nesta versao, para a CPU
				}
			} // FIM DO CICLO DE UMA INSTRUÇÃO
		}
	}
	// ------------------ C P U - fim
	// -----------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------------------

	// ------------------- HW - constituido de CPU e MEMORIA
	// -----------------------------------------------
	public class HW {
		public Memory mem;
		public CPU cpu;

		public HW(int tamMem) {
			mem = new Memory(tamMem);
			cpu = new CPU(mem, true); // true liga debug
		}
	}
	// -------------------------------------------------------------------------------------------------------

	// --------------------H A R D W A R E - fim
	// -------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////

	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// ------------------- SW - inicio - Sistema Operacional
	// -------------------------------------------------

	// ------------------- I N T E R R U P C O E S - rotinas de tratamento
	// ----------------------------------
	public class InterruptHandling {
		private HW hw; // referencia ao hw se tiver que setar algo

		public InterruptHandling(HW _hw) {
			hw = _hw;
		}

		public void handle(Interrupts irpt) {
			// apenas avisa - todas interrupcoes neste momento finalizam o programa
			System.out.println(
					"                                               Interrupcao " + irpt + "   pc: " + hw.cpu.pc);
		}
	}

	// ------------------- C H A M A D A S D E S I S T E M A - rotinas de tratamento
	// ----------------------
	public class SysCallHandling {
		private HW hw; // referencia ao hw se tiver que setar algo
		private Scanner in;

		public SysCallHandling(HW _hw) {
			hw = _hw;
			in = new Scanner(System.in);
		}

		public void stop() { // chamada de sistema indicando final de programa
								// nesta versao cpu simplesmente pára
								// vamos manter o print, pois o boolean cpuStop já encerra o programa
			System.out.println("                                               SYSCALL STOP");
		}

		public void handle() { // chamada de sistema
								// suporta somente IO, com parametros
								// reg[8] = in ou out e reg[9] endereco do inteiro
			System.out.println("SYSCALL pars:  " + hw.cpu.reg[8] + " / " + hw.cpu.reg[9]);

			if (hw.cpu.reg[8] == 1) {
				// leitura - le a entrada do teclado do usuario e guarda em reg[9]
				System.out.println("IN: Leitura do teclado (apenas valores inteiros): ");
				int input = in.nextInt();

				// armazena a entrada no reg[9]
				hw.mem.pos[hw.cpu.reg[9]].p = input;

			} else if (hw.cpu.reg[8] == 2) {
				// escrita - escreve o conteudo da memoria na posicao dada em reg[9]
				System.out.println("OUT:   " + hw.mem.pos[hw.cpu.reg[9]].p);
			} else {
				System.out.println("  PARAMETRO INVALIDO");
			}
		}
	}

	// ------------------ U T I L I T A R I O S D O S I S T E M A
	// -----------------------------------------
	// ------------------ load é invocado a partir de requisição do usuário

	public class MemoryManager {
		private boolean pages[];
		private int pageSize; // Sempre ser um divisor do tamanho da memória.

		public MemoryManager(int pageSize) {
			// Verifica se o tamanho da página é divisor do tamanho da memória
			if (Sistema.this.hw.mem.pos.length % pageSize != 0) {
				throw new IllegalArgumentException("O tamanho da página deve ser um divisor do tamanho da memória.");
			}

			// Inicializa o array de páginas com false
			this.pages = new boolean[Sistema.this.hw.mem.pos.length / pageSize];
			Arrays.fill(this.pages, false);

			// Salva o tamanho da página
			this.pageSize = pageSize;
		}

		public Map<Integer, Integer> jmAlloc(Word[] p) {
			int qtdWords = p.length; // Quantidade de palavras a serem alocadas
			int qtdPages = (int) Math.ceil((double) qtdWords / pageSize); // Calcula o número de páginas necessárias
			Map<Integer, Integer> pageTable = new HashMap<>(); // Tabela de páginas lógica -> física
			int allocatedCount = 0; // Contador de páginas alocadas

			// Percorre as páginas disponíveis para encontrar espaço usando "first fit"
			for (int i = 0; i < pages.length; i++) {
				if (pages[i]) continue; // Página indisponível

				pages[i] = true; // Marca a página como alocada
				pageTable.put(allocatedCount, i); // Mapeia página lógica para frame físico
				allocatedCount++;

				// Copia o conteúdo para a memória
				int startAddress = i * pageSize; // Endereço inicial da página
				for (int offset = 0; offset < pageSize; offset++) {
					int wordIndex = (allocatedCount - 1) * pageSize + offset;
					if (wordIndex >= qtdWords) {
						break;
					}
					Sistema.this.hw.mem.pos[startAddress + offset] = new Word(
							p[wordIndex].opc,
							p[wordIndex].ra,
							p[wordIndex].rb,
							p[wordIndex].p);
				}

				// Verifica se todas as palavras foram alocadas
				if (allocatedCount == qtdPages) {
					return pageTable; // Retorna a tabela de páginas
				}
			}

			// Caso não seja possível alocar todas as páginas, lança uma exceção
			throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
		}

		// não limpa as palavras (memoria sera sobrescrita)
		public List<Integer> jmFree(Map<Integer, Integer> pageTable) {
			List<Integer> freedFrames = new ArrayList<>(); // Lista para armazenar os frames desalocados

			for (int frame : pageTable.values()) { // Itera sobre os frames físicos na tabela de páginas
				if (frame >= 0 && frame < pages.length && pages[frame]) { // Verifica se o frame é válido e está alocado
					pages[frame] = false; // Marca o frame como livre
					freedFrames.add(frame); // Adiciona o frame à lista de desalocados
				}
			}

			return freedFrames; // Retorna os frames desalocados
		}

	}

	public class PCB { // Process Control Block
		public final int pid; // id do
		// public final int ppid; // id do processo pai
		// public final int uid; // id do usuario que criou
		public int pc; // contador de programa
		public int[] reg = new int[10]; // registradores do processo, array de 10 posições
		public int status; // status do processo (running, ready, blocked) podemos realizar um ENUM
		public int priority;
		Map<Integer, Integer> pageTable = new HashMap<>();
		public String programName;
		// public boolean allowInterrupt = true;

		// de alguma forma adicionar evento que o processo está
		// depois teremos que adicionar comunicação entre
		// informações de tempo executando e aguardando
		// recursos controlados pelo processo, como arquivos abertos

		public PCB(int _pid, Map<Integer, Integer> _pageTable, String _name) { // pid é o id do processo
			pid = _pid;
			pc = 0;
			for (int i = 0; i < reg.length; i++) {
				reg[i] = 0;
			}
			name = _name
			Map<Integer, Integer> pageTable = _pageTable; //TODO CLONAR  
			status = 1; // 0 = running, 1 = ready, 2 = blocked
			priority = 0; // prioridade do processo
			programName = _name;

		}

	}

	public class ProcessManager {
		public PCB processRunning;
		public List<PCB> processReady = new ArrayList<>(); // lista de processos prontos
		public List<PCB> processBlocked = new ArrayList<>(); // lista de processos bloqueados
		private int idCounter = 0;

		public boolean createProcess(Program p) {
			MemoryManager mm = new MemoryManager();
			Map<Integer, Integer> pageTable = mm.jmAlloc(p.image);
			idCounter++;
			PCB pcb = new PCB(idCounter, pageTable, p.name);
			processReady.add(pcb);
			return true;
		}

		public void removeProcess(int id){

			// iterar sobre lista de processos prontos e remover o PCB == id
			for(int i = 0; i < processReady.size(); i++){
				if(id == processReady[i]){
					
				}
			}

			// iterar sobre lista de processos bloqueados e remover o PCB == id

			// chamar metodo jmfree() com o map de pages do PCB que encontramos
			
		}

	}

	// carga na memória
	public class Utilities {
		private HW hw;
		private MemoryManager mm;

		public Utilities(HW _hw, MemoryManager _mm) {
			hw = _hw;
			mm = _mm;
		}

		// TODO: fazer método de paginação

		private void loadProgram(Word[] p) {
			Word[] m = hw.mem.pos; // m[] é o array de posições memória do hw
			for (int i = 0; i < p.length; i++) {
				m[i].opc = p[i].opc;
				m[i].ra = p[i].ra;
				m[i].rb = p[i].rb;
				m[i].p = p[i].p;
			}
		}

		// dump da memória
		public void dump(Word w) { // funcoes de DUMP nao existem em hardware - colocadas aqui para facilidade
			System.out.print("[ ");
			System.out.print(w.opc);
			System.out.print(", ");
			System.out.print(w.ra);
			System.out.print(", ");
			System.out.print(w.rb);
			System.out.print(", ");
			System.out.print(w.p);
			System.out.println("  ] ");
		}

		public void dump(int ini, int fim) {
			Word[] m = hw.mem.pos; // m[] é o array de posições memória do hw
			for (int i = ini; i < fim; i++) {
				System.out.print(i);
				System.out.print(":  ");
				dump(m[i]);
			}
		}

		private void loadAndExec(Word[] p) {
			mm.jmAlloc(p); // carga do programa na memoria
			System.out.println("---------------------------------- programa carregado na memoria");
			dump(0, p.length); // dump da memoria nestas posicoes
			hw.cpu.setContext(0); // seta pc para endereço 0 - ponto de entrada dos programas
			System.out.println("---------------------------------- inicia execucao ");
			hw.cpu.run(); // cpu roda programa ate parar
			System.out.println("---------------------------------- memoria após execucao ");
			dump(0, p.length); // dump da memoria com resultado
		}
	}

	public class SO {
		public InterruptHandling ih;
		public SysCallHandling sc;
		public Utilities utils;
		public MemoryManager mm;

		public SO(HW hw) {
			ih = new InterruptHandling(hw); // rotinas de tratamento de int
			sc = new SysCallHandling(hw); // chamadas de sistema
			hw.cpu.setAddressOfHandlers(ih, sc);
			mm = new MemoryManager(16);
			utils = new Utilities(hw, mm); // utilitários do sistema

		}
	}
	// -------------------------------------------------------------------------------------------------------
	// ------------------- S I S T E M A
	// --------------------------------------------------------------------

	public HW hw;
	public SO so;
	public Programs progs;

	public Sistema(int tamMem) {
		hw = new HW(tamMem); // memoria do HW tem tamMem palavras
		so = new SO(hw);
		hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
		progs = new Programs();
	}

	public void run() {

		//so.utils.loadAndExec(progs.retrieveProgram("fatorialV2"));

		Commands cmds = new Commands(progs);

		cmds.waitForCommands();

		// so.utils.loadAndExec(progs.retrieveProgram("fatorial"));
		// fibonacci10,
		// fibonacci10v2,
		// progMinimo,
		// fatorialWRITE, // saida
		// fibonacciREAD, // entrada
		// PB
		// PC, // bubble sort
	}
	// ------------------- S I S T E M A - fim
	// --------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// ------------------- instancia e testa sistema
	public static void main(String args[]) {
		Sistema s = new Sistema(1024);
		s.run();
	}

	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// --------------- P R O G R A M A S - não fazem parte do sistema
	// esta classe representa programas armazenados (como se estivessem em disco)
	// que podem ser carregados para a memória (load faz isto)

	public class Program {
		public String name;
		public Word[] image;

		public Program(String n, Word[] i) {
			name = n;
			image = i;
		}
	}

	public class Programs {

		public Word[] retrieveProgram(String pname) {
			for (Program p : progs) {
				if (p != null & p.name.equals(pname))
					return p.image;
			}
			return null;
		}

		public Program[] progs = {
				new Program("fatorial",
						new Word[] {
								// este fatorial so aceita valores positivos. nao pode ser zero
								// linha coment
								new Word(Opcode.LDI, 0, -1, 7), // 0 r0 é valor a calcular fatorial
								new Word(Opcode.LDI, 1, -1, 1), // 1 r1 é 1 para multiplicar (por r0)
								new Word(Opcode.LDI, 6, -1, 1), // 2 r6 é 1 o decremento
								new Word(Opcode.LDI, 7, -1, 8), // 3 r7 tem posicao 8 para fim do programa
								new Word(Opcode.JMPIE, 7, 0, 0), // 4 se r0=0 pula para r7(=8)
								new Word(Opcode.MULT, 1, 0, -1), // 5 r1 = r1 * r0 (r1 acumula o produto por cada termo)
								new Word(Opcode.SUB, 0, 6, -1), // 6 r0 = r0 - r6 (r6=1) decrementa r0 para proximo
																// termo
								new Word(Opcode.JMP, -1, -1, 4), // 7 vai p posicao 4
								new Word(Opcode.STD, 1, -1, 10), // 8 coloca valor de r1 na posição 10
								new Word(Opcode.STOP, -1, -1, -1), // 9 stop
								new Word(Opcode.DATA, -1, -1, -1) // 10 ao final o valor está na posição 10 da memória
						}),

				new Program("fatorialV2",
						new Word[] {
								new Word(Opcode.LDI, 0, -1, 5), // numero para colocar na memoria, ou pode ser lido
								new Word(Opcode.STD, 0, -1, 19),
								new Word(Opcode.LDD, 0, -1, 19),
								new Word(Opcode.LDI, 1, -1, -1),
								new Word(Opcode.LDI, 2, -1, 13), // SALVAR POS STOP
								new Word(Opcode.JMPIL, 2, 0, -1), // caso negativo pula pro STD
								new Word(Opcode.LDI, 1, -1, 1),
								new Word(Opcode.LDI, 6, -1, 1),
								new Word(Opcode.LDI, 7, -1, 13),
								new Word(Opcode.JMPIE, 7, 0, 0), // POS 9 pula para STD (Stop-1)
								new Word(Opcode.MULT, 1, 0, -1),
								new Word(Opcode.SUB, 0, 6, -1),
								new Word(Opcode.JMP, -1, -1, 9), // pula para o JMPIE
								new Word(Opcode.STD, 1, -1, 18),
								new Word(Opcode.LDI, 8, -1, 2), // escrita
								new Word(Opcode.LDI, 9, -1, 18), // endereco com valor a escrever
								new Word(Opcode.SYSCALL, -1, -1, -1),
								new Word(Opcode.STOP, -1, -1, -1), // POS 17
								new Word(Opcode.DATA, -1, -1, -1), // POS 18
								new Word(Opcode.DATA, -1, -1, -1) } // POS 19
				),

				new Program("progMinimo",
						new Word[] {
								new Word(Opcode.LDI, 0, -1, 999),
								new Word(Opcode.STD, 0, -1, 8),
								new Word(Opcode.STD, 0, -1, 9),
								new Word(Opcode.STD, 0, -1, 10),
								new Word(Opcode.STD, 0, -1, 11),
								new Word(Opcode.STD, 0, -1, 12),
								new Word(Opcode.STOP, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1), // 7
								new Word(Opcode.DATA, -1, -1, -1), // 8
								new Word(Opcode.DATA, -1, -1, -1), // 9
								new Word(Opcode.DATA, -1, -1, -1), // 10
								new Word(Opcode.DATA, -1, -1, -1), // 11
								new Word(Opcode.DATA, -1, -1, -1), // 12
								new Word(Opcode.DATA, -1, -1, -1) // 13
						}),

				new Program("fibonacci10",
						new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
								new Word(Opcode.LDI, 1, -1, 0),
								new Word(Opcode.STD, 1, -1, 20),
								new Word(Opcode.LDI, 2, -1, 1),
								new Word(Opcode.STD, 2, -1, 21),
								new Word(Opcode.LDI, 0, -1, 22),
								new Word(Opcode.LDI, 6, -1, 6),
								new Word(Opcode.LDI, 7, -1, 31),
								new Word(Opcode.LDI, 3, -1, 0),
								new Word(Opcode.ADD, 3, 1, -1),
								new Word(Opcode.LDI, 1, -1, 0),
								new Word(Opcode.ADD, 1, 2, -1),
								new Word(Opcode.ADD, 2, 3, -1),
								new Word(Opcode.STX, 0, 2, -1),
								new Word(Opcode.ADDI, 0, -1, 1),
								new Word(Opcode.SUB, 7, 0, -1),
								new Word(Opcode.JMPIG, 6, 7, -1),
								new Word(Opcode.STOP, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1), // POS 20
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1) // ate aqui - serie de fibonacci ficara armazenada
						}),

				new Program("fibonacci10v2",
						new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
								new Word(Opcode.LDI, 1, -1, 0),
								new Word(Opcode.STD, 1, -1, 20),
								new Word(Opcode.LDI, 2, -1, 1),
								new Word(Opcode.STD, 2, -1, 21),
								new Word(Opcode.LDI, 0, -1, 22),
								new Word(Opcode.LDI, 6, -1, 6),
								new Word(Opcode.LDI, 7, -1, 31),
								new Word(Opcode.MOVE, 3, 1, -1),
								new Word(Opcode.MOVE, 1, 2, -1),
								new Word(Opcode.ADD, 2, 3, -1),
								new Word(Opcode.STX, 0, 2, -1),
								new Word(Opcode.ADDI, 0, -1, 1),
								new Word(Opcode.SUB, 7, 0, -1),
								new Word(Opcode.JMPIG, 6, 7, -1),
								new Word(Opcode.STOP, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1), // POS 20
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1) // ate aqui - serie de fibonacci ficara armazenada
						}),
				new Program("fibonacciREAD",
						new Word[] {
								// mesmo que prog exemplo, so que usa r0 no lugar de r8
								new Word(Opcode.LDI, 8, -1, 1), // leitura
								new Word(Opcode.LDI, 9, -1, 55), // endereco a guardar o tamanho da serie de fib a gerar
																	// - pode ser de 1 a 20
								new Word(Opcode.SYSCALL, -1, -1, -1),
								new Word(Opcode.LDD, 7, -1, 55),
								new Word(Opcode.LDI, 3, -1, 0),
								new Word(Opcode.ADD, 3, 7, -1),
								new Word(Opcode.LDI, 4, -1, 36), // posicao para qual ira pular (stop) *
								new Word(Opcode.LDI, 1, -1, -1), // caso negativo
								new Word(Opcode.STD, 1, -1, 41),
								new Word(Opcode.JMPIL, 4, 7, -1), // pula pra stop caso negativo *
								new Word(Opcode.JMPIE, 4, 7, -1), // pula pra stop caso 0
								new Word(Opcode.ADDI, 7, -1, 41), // fibonacci + posição do stop
								new Word(Opcode.LDI, 1, -1, 0),
								new Word(Opcode.STD, 1, -1, 41), // 25 posicao de memoria onde inicia a serie de
																	// fibonacci gerada
								new Word(Opcode.SUBI, 3, -1, 1), // se 1 pula pro stop
								new Word(Opcode.JMPIE, 4, 3, -1),
								new Word(Opcode.ADDI, 3, -1, 1),
								new Word(Opcode.LDI, 2, -1, 1),
								new Word(Opcode.STD, 2, -1, 42),
								new Word(Opcode.SUBI, 3, -1, 2), // se 2 pula pro stop
								new Word(Opcode.JMPIE, 4, 3, -1),
								new Word(Opcode.LDI, 0, -1, 43),
								new Word(Opcode.LDI, 6, -1, 25), // salva posição de retorno do loop
								new Word(Opcode.LDI, 5, -1, 0), // salva tamanho
								new Word(Opcode.ADD, 5, 7, -1),
								new Word(Opcode.LDI, 7, -1, 0), // zera (inicio do loop)
								new Word(Opcode.ADD, 7, 5, -1), // recarrega tamanho
								new Word(Opcode.LDI, 3, -1, 0),
								new Word(Opcode.ADD, 3, 1, -1),
								new Word(Opcode.LDI, 1, -1, 0),
								new Word(Opcode.ADD, 1, 2, -1),
								new Word(Opcode.ADD, 2, 3, -1),
								new Word(Opcode.STX, 0, 2, -1),
								new Word(Opcode.ADDI, 0, -1, 1),
								new Word(Opcode.SUB, 7, 0, -1),
								new Word(Opcode.JMPIG, 6, 7, -1), // volta para o inicio do loop
								new Word(Opcode.STOP, -1, -1, -1), // POS 36
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1), // POS 41
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1)
						}),
				new Program("PB",
						new Word[] {
								// dado um inteiro em alguma posição de memória,
								// se for negativo armazena -1 na saída; se for positivo responde o fatorial do
								// número na saída
								new Word(Opcode.LDI, 0, -1, 7), // numero para colocar na memoria
								new Word(Opcode.STD, 0, -1, 50),
								new Word(Opcode.LDD, 0, -1, 50),
								new Word(Opcode.LDI, 1, -1, -1),
								new Word(Opcode.LDI, 2, -1, 13), // SALVAR POS STOP
								new Word(Opcode.JMPIL, 2, 0, -1), // caso negativo pula pro STD
								new Word(Opcode.LDI, 1, -1, 1),
								new Word(Opcode.LDI, 6, -1, 1),
								new Word(Opcode.LDI, 7, -1, 13),
								new Word(Opcode.JMPIE, 7, 0, 0), // POS 9 pula pra STD (Stop-1)
								new Word(Opcode.MULT, 1, 0, -1),
								new Word(Opcode.SUB, 0, 6, -1),
								new Word(Opcode.JMP, -1, -1, 9), // pula para o JMPIE
								new Word(Opcode.STD, 1, -1, 15),
								new Word(Opcode.STOP, -1, -1, -1), // POS 14
								new Word(Opcode.DATA, -1, -1, -1) // POS 15
						}),
				new Program("PC",
						new Word[] {
								// Para um N definido (10 por exemplo)
								// o programa ordena um vetor de N números em alguma posição de memória;
								// ordena usando bubble sort
								// loop ate que não swap nada
								// passando pelos N valores
								// faz swap de vizinhos se da esquerda maior que da direita
								new Word(Opcode.LDI, 7, -1, 5), // TAMANHO DO BUBBLE SORT (N)
								new Word(Opcode.LDI, 6, -1, 5), // aux N
								new Word(Opcode.LDI, 5, -1, 46), // LOCAL DA MEMORIA
								new Word(Opcode.LDI, 4, -1, 47), // aux local memoria
								new Word(Opcode.LDI, 0, -1, 4), // colocando valores na memoria
								new Word(Opcode.STD, 0, -1, 46),
								new Word(Opcode.LDI, 0, -1, 3),
								new Word(Opcode.STD, 0, -1, 47),
								new Word(Opcode.LDI, 0, -1, 5),
								new Word(Opcode.STD, 0, -1, 48),
								new Word(Opcode.LDI, 0, -1, 1),
								new Word(Opcode.STD, 0, -1, 49),
								new Word(Opcode.LDI, 0, -1, 2),
								new Word(Opcode.STD, 0, -1, 50), // colocando valores na memoria até aqui - POS 13
								new Word(Opcode.LDI, 3, -1, 25), // Posicao para pulo CHAVE 1
								new Word(Opcode.STD, 3, -1, 99),
								new Word(Opcode.LDI, 3, -1, 22), // Posicao para pulo CHAVE 2
								new Word(Opcode.STD, 3, -1, 98),
								new Word(Opcode.LDI, 3, -1, 38), // Posicao para pulo CHAVE 3
								new Word(Opcode.STD, 3, -1, 97),
								new Word(Opcode.LDI, 3, -1, 25), // Posicao para pulo CHAVE 4 (não usada)
								new Word(Opcode.STD, 3, -1, 96),
								new Word(Opcode.LDI, 6, -1, 0), // r6 = r7 - 1 POS 22
								new Word(Opcode.ADD, 6, 7, -1),
								new Word(Opcode.SUBI, 6, -1, 1), // ate aqui
								new Word(Opcode.JMPIEM, -1, 6, 97), // CHAVE 3 para pular quando r7 for 1 e r6 0 para
																	// interomper o loop de vez do programa
								new Word(Opcode.LDX, 0, 5, -1), // r0 e ra pegando valores das posições da memoria POS
																// 26
								new Word(Opcode.LDX, 1, 4, -1),
								new Word(Opcode.LDI, 2, -1, 0),
								new Word(Opcode.ADD, 2, 0, -1),
								new Word(Opcode.SUB, 2, 1, -1),
								new Word(Opcode.ADDI, 4, -1, 1),
								new Word(Opcode.SUBI, 6, -1, 1),
								new Word(Opcode.JMPILM, -1, 2, 99), // LOOP chave 1 caso neg procura prox
								new Word(Opcode.STX, 5, 1, -1),
								new Word(Opcode.SUBI, 4, -1, 1),
								new Word(Opcode.STX, 4, 0, -1),
								new Word(Opcode.ADDI, 4, -1, 1),
								new Word(Opcode.JMPIGM, -1, 6, 99), // LOOP chave 1 POS 38
								new Word(Opcode.ADDI, 5, -1, 1),
								new Word(Opcode.SUBI, 7, -1, 1),
								new Word(Opcode.LDI, 4, -1, 0), // r4 = r5 + 1 POS 41
								new Word(Opcode.ADD, 4, 5, -1),
								new Word(Opcode.ADDI, 4, -1, 1), // ate aqui
								new Word(Opcode.JMPIGM, -1, 7, 98), // LOOP chave 2
								new Word(Opcode.STOP, -1, -1, -1), // POS 45
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1),
								new Word(Opcode.DATA, -1, -1, -1)
						})
		};
	}

	public class Commands {
		public interface Comando {
			public void execute(String[] args);
		}
		
		private Programs progs;
		public Commands(Programs programs) {
			this.progs = programs;
			
		}
	
		private Comando newCmd = new Comando() {
			public void execute(String[] args) {
				String processName = args[0];
				Word[] program = progs.retrieveProgram(processName.strip());
				if (program == null) {
					System.out.println("Programa não encontrado: " + processName);
					
					for (Program p : progs.progs) {
						System.out.println("Programa disponível: " + p.name);
					}

					return;
				}
				try {
					Map<Integer, Integer> pageTable = Sistema.this.so.mm.jmAlloc(program);
					PCB newProcess = new PCB(Sistema.this.so.mm.pages.length, -1, pageTable);
					newProcess.programName = processName;
					Sistema.this.so.utils.dump(0, program.length);
					Sistema.this.so.utils.loadAndExec(program);
					System.out.println("Processo criado: " + processName);
				} catch (OutOfMemoryError e) {
					System.out.println("Erro ao criar processo: memória insuficiente.");
				}
			}
		};

		public void waitForCommands() {
			Scanner scanner = new Scanner(System.in);
			while (true) {
				System.out.print("Digite um comando: ");
				String typed = scanner.nextLine();
				String[] args = typed.split(" ");
				String command = args[0].toLowerCase(); // Comando em minúsculas para comparação
				args = Arrays.copyOfRange(args, 1, args.length); // Ignora o primeiro argumento (comando)
				if (command.equals("exit")) {
					System.out.println("Saindo do programa.");
					break;
				} else if (command.equals("new")) {
					newCmd.execute(args); // Executa o comando 1
				} else {
					System.out.println("Comando inválido. Tente novamente.");
				}
			}
			scanner.close(); // Fecha o scanner quando terminar
		}
	}
}