
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Этот класс создает агента, который действует как инициатор.
 * Его роль заключается в обработке входящих заявок на предмет (в данном случае работу), созданный агентом.
 * Агент хочет наименьшее возможное значение, чтобы агент платил наименьшую возможную сумму, чтобы выполнить работу.
 * Чем больше ответивших, тем выше вероятность уменьшения платежа.
 *
 * Аргументы (обязательные): «Название должности (строка), оплата (целое число)»
 * Первый аргумент — это название работы, а второй аргумент — это стартовый платеж за работу.
 */
public class CompanyAgent extends Agent {
	private static final long serialVersionUID = 1L;
	private Hashtable<String, Integer> availableJobs;
	private ArrayList<Integer> paymentList;
	private DFHelper helper;
	private String jobTitle = null;
	private String payment = null;
	private int initialPayment;

	public int IdCompany;
	public int Coordinate_X ,Coordinate_Y; // Координаты
	public int Demand; // Спрос узла, если клиент
	public boolean IsRouted;
	private boolean IsDepot; //Если склад


	/**
	 * Регистрирует агента у посредника каталога как компанию,
	 * и подготавливает агента к исходящему сообщению.
	 */
	protected void setup() {
		helper = DFHelper.getInstance();
		availableJobs = new Hashtable<String, Integer>();

		Object[] args = getArguments();
		if (args.length == 2) {
			jobTitle = (String) args[0];
			payment = (String) args[1];
			/*Coordinate_X = Integer.parseInt((String) args[2]);
			Coordinate_Y = Integer.parseInt((String) args[3]);
			if (!Boolean.valueOf((String) args[4]))
			{
				Demand = Integer.parseInt((String) args[5]);;
				IsRouted = Boolean.valueOf((String) args[6]);;
			}
			else{
				IsDepot = Boolean.valueOf((String) args[4]);
			}*/

			if (payment.matches("^\\d+$")) {
				initialPayment = Double.valueOf(payment).intValue();

				updateJobListings(jobTitle, initialPayment);

				paymentList = new ArrayList<Integer>();
				paymentList.add(initialPayment);

				ServiceDescription serviceDescription = new ServiceDescription();
				serviceDescription.setType("Company");
				serviceDescription.setName(getLocalName());
				helper.register(this, serviceDescription);
			} else {
				System.out.println("Платеж должен быть положительным числом (например, 100).");
				System.out.println("Программа остановлена: " + this.getAID().getName());
				doDelete();
			}
		} else {
			System.out.println("Требуются два аргумента. Укажите аргументы в формате \"Должность, Оплата, Координата Х, Координата У, Это Депо,\", где Оплата – это число (например, 100)");
			System.out.println("Программа остановлена: " + this.getAID().getName());
			doDelete();
		}

		addBehaviour(new ContractNetInitiator(this, null) {

			private int globalResponses = 0;


			/**
			 * Инициируется при запуске и отправляет сообщение CFP агентам, указанным как тип «Перевозчик».
			 * Сообщение содержит название работы, а также ее оплату.
			 */
			public Vector<ACLMessage> prepareCfps(ACLMessage init) {
				init = new ACLMessage(ACLMessage.CFP);
				Vector<ACLMessage> messages = new Vector<ACLMessage>();

				AID[] agents = helper.searchDF(getAgent(), "Carrier");

				System.out.println("Фасилитатором были найдены агенты с функцией «Перевозчик»:");
				for (AID agent : agents) {
					System.out.println(agent.getName());
					init.addReceiver(new AID((String) agent.getLocalName(), AID.ISLOCALNAME));
				}
				System.out.println();

				if (agents.length == 0) {
					System.out.println("Агенты, соответствующие типу, не найдены. Прекращение:" + getAgent().getAID().getName());
					helper.killAgent(getAgent());
				} else {
					init.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
					init.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					init.setContent(jobTitle + "|" + payment);

					messages.addElement(init);
				}

				return messages;
			}

			protected void handlePropose(ACLMessage propose, Vector v) {
				System.out.println(propose.getSender().getName() + " находится " + propose.getContent() + "  рассточние для выполнения работы: \"" + jobTitle + "\".");
			}

			protected void handleRefuse(ACLMessage refuse) {
				globalResponses++;
				System.out.println(refuse.getSender().getName() + " не желает предлагать цену ниже.");
				helper.removeReceiverAgent(refuse.getSender(), refuse);
			}

			protected void handleFailure(ACLMessage failure) {
				globalResponses++;
				System.out.println(failure.getSender().getName() + " не смог ответить");
				helper.removeReceiverAgent(failure.getSender(), failure);
			}

			/**
			 * Как только респондент отвечает INFORM, инициатор знает, что задание
			 * был принят, поэтому все агенты, участвовавшие в аукционе, могут быть расторгнуты.
			 */
			protected void handleInform(ACLMessage inform) {
				globalResponses++;
				System.out.println("\n" + getAID().getName() + " больше не имеет свободных рабочих мест.");
				availableJobs.remove(jobTitle);
				for (Agent agent : helper.getRegisteredAgents()) {
					helper.killAgent(agent);
				}
			}

			/**
			 * Обрабатывает ответы от других респондентов и решает, отправлять ли новый CFP (если осталось несколько респондентов),
			 * или принять предложение ответившего (если ответивший остался в аукционе один).
			 */
			protected void handleAllResponses(Vector responses, Vector acceptances) {
				int agentsLeft = responses.size() - globalResponses;
				globalResponses = 0;

				System.out.println("\n" + getAID().getName() + " обрабатывает все: Получено " + agentsLeft + " ответов.");

				int bestProposal = Integer.parseInt(payment);
				ACLMessage reply = new ACLMessage(ACLMessage.CFP);
				Vector<ACLMessage> cfpVector = new Vector<ACLMessage>();
				Enumeration<?> e = responses.elements();
				ArrayList<ACLMessage> responderList = new ArrayList<ACLMessage>();

				ACLMessage Viner = new ACLMessage();

				while (e.hasMoreElements()) {
					ACLMessage msg = (ACLMessage) e.nextElement();
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						int proposal = Integer.parseInt(msg.getContent());
						reply = msg.createReply();
						reply.setPerformative(ACLMessage.CFP);
						if (proposal < bestProposal) {
							Viner = msg.createReply();
							Viner.setPerformative(ACLMessage.REJECT_PROPOSAL);
							responderList.add(reply);
							bestProposal = proposal;

						}
						cfpVector.addElement(reply);
					}
				}


				if (agentsLeft > 1) {
					paymentList.add(bestProposal);

					for (int i = 0; i < responderList.size(); i++) {
						responderList.get(i).setContent(jobTitle + "|" + bestProposal);
						cfpVector.set(i, responderList.get(i));
					}

					System.out.println(agentsLeft + "Компанией была рассмотрела все варианты выбора перевозчика, лучшим оказался");
					System.out.println(getAID().getName() + " выдает CFP с оплатой в размере $" + paymentList.get(paymentList.size() - 1) + ".\n");
					newIteration(cfpVector);
					acceptances.addElement(Viner);

				} else if (agentsLeft == 1) {
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					if (bestProposal <= paymentList.get(paymentList.size() - 1)) {
						reply.setContent(jobTitle + "|" + bestProposal);
						reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					}
					acceptances.addElement(reply);
				} else {
					System.out.println("Ни один агент не согласился на эту работу.");
				}
			}

		});
	}

/**
 * Добавляет новое задание в хеш-таблицу.
 * @param jobTitle - название вакансии
 * @param payment - оплата за работу
 */
	public void updateJobListings(final String jobTitle, final int payment) {
		addBehaviour(new OneShotBehaviour() {
			private static final long serialVersionUID = 1L;

			public void action() {
				availableJobs.put(jobTitle, new Integer(payment));
				System.out.println(getAID().getName() +  "выдал новое задание: \"" + jobTitle+ "\", начиная с $" + payment + ".\n");
			}
		});
	}

}
