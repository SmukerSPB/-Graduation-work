

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;

/**
 * Этот класс создает агента, который действует как ответчик.
 * Его роль заключается в том, чтобы сделать ставку на предмет (в данном случае работу).
 * Агент пытается получить максимально возможную ценность за выполнение работы,
 * но когда есть другие ответчики, они будут занижать ставки друг друга в надежде, что другие ответчики проиграют.
 *
 * Аргументы (необязательно): «Процент (целое число)»
 * Аргумент определяет, насколько низко агент готов пойти на основе первоначального платежа.
 * Если аргумент не указан (или формат неверен), будет использоваться значение по умолчанию: 50.
 */
public class CarrierAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private DFHelper helper;
    private int initialPayment = 0;
    private int percentage = 50;

    public int VehId; // ID автомобиля
    public ArrayList<Node> Route = new ArrayList<Node>(); // Массив точек обслуживания
    public int Capacity; // Емкость транспорта
    public int Load; // Нагрузка на автомобиль
    public int CurLoc; // Ограничения
    public boolean Closed; // Автомобиль закрыт для приема нового товара

    Random generate = new Random();
    int Distance = generate.nextInt(1000) + 1;
    /**
     * Проверяем, присутствуем нарушение емкости
     */
    public boolean CheckIfFits(int dem)
    {
        return ((Load + dem <= Capacity));
    }
    /**
     * Регистрирует агента у посредника каталога в качестве перевозчика,
     * и подготавливает агента к входящему сообщению.
     */
    protected void setup() {
        helper = DFHelper.getInstance();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Carrier");
        serviceDescription.setName(getLocalName());
        helper.register(this, serviceDescription);

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String percentageArg = (String) args[0];
            if (percentageArg.matches("^\\d+$")) {
                percentage = Integer.parseInt(percentageArg);
            }
        }

        final String IP = FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET;
        MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchProtocol(IP),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));

        SequentialBehaviour sequential = new SequentialBehaviour();
        addBehaviour(sequential);
        ParallelBehaviour parallel = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        sequential.addSubBehaviour(parallel);
        parallel.addSubBehaviour(new CustomContractNetResponder(this, template));
    }

    private class CustomContractNetResponder extends SSResponderDispatcher {

        private CustomContractNetResponder(Agent agent, MessageTemplate template) {
            super(agent, template);
        }

        protected Behaviour createResponder(ACLMessage message) {
            return new SSIteratedContractNetResponder(myAgent, message) {

                /**
                 * Отвечает на сообщение CFP от инициатора сообщением PROPOSE/REFUSE.
                 * Если платеж слишком мал для агента, он отклоняется с сообщением REFUSE,
                 * в противном случае агент ответит сообщением PROPOSE.
                 */
                protected ACLMessage handleCfp(ACLMessage cfp) {
                    int payment = 0;
                    int backupPayment = 0;
                    try {
                        payment = Integer.parseInt(cfp.getContent().substring(cfp.getContent().lastIndexOf("|") + 1));
                        backupPayment = payment;
                    } catch (Exception e) {
                        System.out.println(getAID().getName() + " не могу получить маршрут!");
                    }

                    if (initialPayment == 0) {
                        initialPayment = payment;
                    }

                    /*Random generate = new Random();

                    int upperBound;
                    int length = String.valueOf(payment).length();

                    switch (length) {
                        case 1:
                            upperBound = 1;
                            break;
                        case 2:
                            upperBound = 5;
                            break;
                        case 3:
                            upperBound = 50;
                            break;
                        default:
                            upperBound = 300;
                            break;
                    }

                    int randomNumber = generate.nextInt(upperBound) + 1;
                    int lowerBound = (int) (initialPayment * (percentage / 100.0f));

                    if (randomNumber != 1 && (payment - randomNumber) > lowerBound) {
                        payment = (payment - randomNumber);
                    } else {
                        payment = 0;
                    }*/
                    Distance
                    ACLMessage response = cfp.createReply();

                    if (payment > 0) {
                        response.setPerformative(ACLMessage.PROPOSE);
                        if (helper.getRespondersRemaining() == 1) {
                            response.setContent(String.valueOf(backupPayment));
                        } else {
                            response.setContent(String.valueOf(payment));
                        }
                    } else {
                        upperBound = generate.nextInt(3000) + 1000;
                        doWait(upperBound);

                        if (helper.getRespondersRemaining() == 1) {
                            response.setPerformative(ACLMessage.PROPOSE);
                            response.setContent(String.valueOf(backupPayment));
                        } else {
                            response.setPerformative(ACLMessage.REFUSE);
                        }
                    }
                    return response;
                }
                /**
                 * Агент получил сообщение ACCEPT_PROPOSAL и выиграл аукцион.
                 */
                protected ACLMessage handleAcceptProposal(ACLMessage msg, ACLMessage propose, ACLMessage accept) {
                    if (msg != null) {
                        String jobTitle = null;
                        int payment = 0;
                        try {
                            jobTitle = accept.getContent().substring(0, accept.getContent().indexOf("|"));
                            payment = Integer.parseInt(accept.getContent().substring(accept.getContent().lastIndexOf("|") + 1));
                        } catch (Exception e) {
                        }

                        System.out.println(getAID().getName() + " принял работу \"" + jobTitle + "\" от "
                                + accept.getSender().getName() + ", и получил " + payment + " за его завершение.");
                        ACLMessage inform = accept.createReply();
                        inform.setPerformative(ACLMessage.INFORM);
                        return inform;
                    } else {
                        ACLMessage failure = accept.createReply();
                        failure.setPerformative(ACLMessage.FAILURE);
                        return failure;
                    }
                }

                protected void handleRejectProposal(ACLMessage msg, ACLMessage propose, ACLMessage reject) {
                    System.out.println(reject.getSender().getName() + " не могу продолжить " + getAID().getName() +
                            " работу из-за неожиданных результатов.");
                }





            };
        }
    }



}
