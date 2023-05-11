
import java.util.ArrayList;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import org.w3c.dom.*;

/**
 * Этот класс создается при инициализации агента-перевозчика (ответчика),
 ** Агент Компании (инициатор) или Агент Сотрудника. Его цель состоит в том, чтобы предоставить агентам
 ** информацию, относящейся к процессу поиска оптимального маршрута, а также прекращению деятельности агентов.
 */
public class DFHelper extends Agent {
	private static final long serialVersionUID = 1L;
	private int respondersRemaining = 0;

	private static DFHelper instance = null;
	private ArrayList<Agent> registeredAgents = new ArrayList<Agent>();

	private DFHelper() {
	}

	public static synchronized DFHelper getInstance() {
		if (instance == null) {
			instance = new DFHelper();
		}
		return instance;
	}


	/**
	 * Зарегистрируйте нового агента с заданными свойствами
	 * @param agent - агент
	 * @param serviceDescription — свойства для агента
	 */
	public void register(Agent agent, ServiceDescription serviceDescription) {
		DFAgentDescription dfAgentDescription = new DFAgentDescription();
		dfAgentDescription.setName(getAID());
		dfAgentDescription.addServices(serviceDescription);

		try {
			registeredAgents.add(agent);
			DFService.register(agent, dfAgentDescription);
			System.out.println(agent.getName() + " зарегестрировался как " + serviceDescription.getType() + ".");
		} catch (FIPAException e) {
			e.printStackTrace();
		}

	}


	/**
	 * Поиск всех агентов определенного типа.
	 * @param agent - агент
	 * Сервис @param - введите для поиска
	 * @return - массив AID (если есть) или null (если нет)
	 */
	public AID[] searchDF(Agent agent, String service) {
		DFAgentDescription dfAgentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setType(service);
		dfAgentDescription.addServices(serviceDescription);

		SearchConstraints findAll = new SearchConstraints();
		findAll.setMaxResults(new Long(-1));
		
		try {
			DFAgentDescription[] result = DFService.search(agent, dfAgentDescription, findAll);
			AID[] agents = new AID[result.length];
			for (int i = 0; i < result.length; i++) {
				agents[i] = result[i].getName();
				respondersRemaining++;
			}
			return agents;
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * Удаляет получателя из текущего аукциона, но не прекращает его.
	 * @param agent - агент для удаления
	 * @param msg — сообщение, с которым оно связано
	 */
	public void removeReceiverAgent(AID agent, ACLMessage msg) {
		respondersRemaining--;
		System.out.println(agent.getName() + " Участник был удален");
		msg.removeReceiver(agent);
	}

	/**
	 * Добовляе получателя из текущего аукциона, но не прекращает его.
	 * @param agent - агент для удаления
	 * @param msg — сообщение, с которым оно связано
	 */
	public void PlusReceiverAgent(AID agent, ACLMessage msg) {
		respondersRemaining++;
	}





	/**
 * Отменяет регистрацию и убивает указанного агента, но заявляет, что он просто "ушел" (брутально)
 * @param agent - агент для убийства
 */
	public void killAgent(Agent agent) {
		try {
			System.out.println(agent.getAID().getName() + " Покинул систему.");
			DFService.deregister(agent);
			agent.doDelete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Возвращает общее количество респондентов, оставшихся на карте.
	 * @возврат - ^
	 */
	public int getRespondersRemaining() {
		return respondersRemaining;
	}


	/**
	 * Возвращает список зарегистрированных агентов.
	 * @возврат - ^
	 */
	public ArrayList<Agent> getRegisteredAgents() {
		return registeredAgents;
	}

	private void CreateCard(){
		Random ran = new Random(151190);

		//Problem Parameters
		int NoOfCustomers = (int) registeredAgents.stream().count();
		int NoOfVehicles = 5;
		int VehicleCap = 50;

		//Depot Coordinates
		int Depot_x = 50;
		int Depot_y = 50;

		//Tabu Parameter
		int TABU_Horizon = 10;

		//Initialise
		//Create Random Customers
		Node[] Nodes = new Node[NoOfCustomers + 1];
		Node depot = new Node(Depot_x, Depot_y);

		Nodes[0] = depot;
		for (int i = 1; i <= NoOfCustomers; i++) {
			Nodes[i] = new Node(i, //Id ) is reserved for depot
					ran.nextInt(100), //Random Cordinates
					ran.nextInt(100),
					4 + ran.nextInt(7)  //Random Demand
			);
		}

		double[][] distanceMatrix = new double[NoOfCustomers + 1][NoOfCustomers + 1];
		double Delta_x, Delta_y;
		for (int i = 0; i <= NoOfCustomers; i++) {
			for (int j = i + 1; j <= NoOfCustomers; j++) //The table is summetric to the first diagonal
			{                                      //Use this to compute distances in O(n/2)

				Delta_x = (Nodes[i].Node_X - Nodes[j].Node_X);
				Delta_y = (Nodes[i].Node_Y - Nodes[j].Node_Y);

				double distance = Math.sqrt((Delta_x * Delta_x) + (Delta_y * Delta_y));

				distance = Math.round(distance);                //Distance is Casted in Integer
				//distance = Math.round(distance*100.0)/100.0; //Distance in double

				distanceMatrix[i][j] = distance;
				distanceMatrix[j][i] = distance;
			}
		}
		int printMatrix = 1; //If we want to print diastance matrix

		if (printMatrix == 1){
			for (int i = 0; i <= NoOfCustomers; i++) {
				for (int j = 0; j <= NoOfCustomers; j++) {
					System.out.print(distanceMatrix[i][j] + "  ");
				}
				System.out.println();
			}
		}

	}

}
