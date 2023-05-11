import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws ControllerException {
        //  Runtime rt = Runtime.instance();

        int Number_of_Agent = 0;

        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.GUI, "true");
        ContainerController cc = rt.createMainContainer(p);
       // AgentController  AgentSystem = null;
        ArrayList<AgentController> CerrierList = new ArrayList<AgentController>();
        int Variant =0;
        int MenuSystem = 0;

        // Инициализация параметров для матрицы
        Random ran = new Random(151190);

        //Параметры задачи
        int NoOfCustomers =0,NoOfVehicles =0, VehicleCap = 0;
        // Количество клиентов, Количестов транспортных средств, Вместимость автомобиля

        Scanner ScannerMenu = new Scanner(System.in);
        System.out.println("Количестов клиентов");
        NoOfCustomers = ScannerMenu.nextInt();  // Количество клиентов
        System.out.println("Количестов транспортных средств");
        NoOfVehicles = ScannerMenu.nextInt();   // Количестов транспортных средств
        System.out.println("Вместимость автомобиля");
        VehicleCap = ScannerMenu.nextInt();     // Вместимость автомобиля



        //Координаты депо
        int Depot_x = 50;
        int Depot_y = 50;

        //Табу параметр
        int TABU_Horizon = 10;

        //Инициализация
        //Создание рандобных клиентов
        Node[] Nodes = new Node[NoOfCustomers + 1];
        Node depot = new Node(Depot_x, Depot_y);

        Nodes[0] = depot;
        //Резервация свободного депо

        //Задаем рандомная координаты для всех клиентов
        for (int i = 1; i <= NoOfCustomers; i++) {
            Nodes[i] = new Node(i, //Id )
                    ran.nextInt(100),
                    ran.nextInt(100),
                    4 + ran.nextInt(7)
            );
        }

        double[][] distanceMatrix = new double[NoOfCustomers + 1][NoOfCustomers + 1];
        double Delta_x, Delta_y;
        for (int i = 0; i <= NoOfCustomers; i++) {
            for (int j = i + 1; j <= NoOfCustomers; j++)
            {

                Delta_x = (Nodes[i].Node_X - Nodes[j].Node_X);
                Delta_y = (Nodes[i].Node_Y - Nodes[j].Node_Y);

                double distance = Math.sqrt((Delta_x * Delta_x) + (Delta_y * Delta_y));

                distance = Math.round(distance); //Целочисленная дистанция
                //distance = Math.round(distance*100.0)/100.0; // Дробная динстанция

                distanceMatrix[i][j] = distance;
                distanceMatrix[j][i] = distance;
            }
        }


        /*for (int i = 0; i <= NoOfCustomers; i++) {
            for (int j = 0; j <= NoOfCustomers; j++) {
                System.out.print(distanceMatrix[i][j] + "  ");
            }
            System.out.println();
        }*/

        Timer Stopwatch = new Timer();
        Solution s = new Solution(NoOfCustomers, NoOfVehicles, VehicleCap);

        System.out.print("\u001b[H\u001b[2J");
        System.out.flush();
        System.out.println("--------------- Функциональное меню  ------------------");
        System.out.println("* 1. Жадный алгоритм                                  *");
        System.out.println("* 2. Локальный алгоритм                               *");
        System.out.println("* 3. Глобальный алгоритм                              *");
        System.out.println("* 4. Табу алгоритм                                    *");
        System.out.println("* 5. Задайте количество Агентов в системе             *");
        System.out.println("* 6. Задайте количество Агентов в системе             *");
        System.out.println("* 7. Зарегистрировать агентов в системе               *");
        System.out.println("* 8. Начать тогрги между агентами                     *");
        System.out.println("* 9. Выход                                            *");
        System.out.println("-------------------------------------------------------");
        System.out.println("Для старта выберете пункт 1: ");

        do {


            MenuSystem = ScannerMenu.nextInt();

            switch (MenuSystem) {
                case 1:
                    System.out.println("Решение маршрутизации транспортных средств (CVRP) для "+NoOfCustomers+
                            " клинтов "+NoOfVehicles+" и транспорта c ограничениями по весу " +VehicleCap + "\n");
                    Stopwatch.Start();
                    s.GreedySolution(Nodes, distanceMatrix);
                    Stopwatch.Stop();
                    s.SolutionPrint("Жадный алгоритм");
                    Draw.drawRoutes(s, "Жадный алгоритм");
                    System.out.println("Прошло времени, мс: " + Stopwatch.Result());

                    break;
                case 2:
                    System.out.println("Решение маршрутизации транспортных средств (CVRP) для "+NoOfCustomers+
                            " клинтов "+NoOfVehicles+" и транспорта c ограничениями по весу " +VehicleCap + "\n");
                    Stopwatch.Start();
                    s.InterRouteLocalSearch(Nodes, distanceMatrix);
                    Stopwatch.Stop();
                    s.SolutionPrint("Локальный алгоритм");
                    Draw.drawRoutes(s, "Локальный алгоритм");
                    System.out.println("Прошло времени, мс: " + Stopwatch.Result());

                    break;
                case 3:
                    System.out.println("Решение маршрутизации транспортных средств (CVRP) для "+NoOfCustomers+
                            " клинтов "+NoOfVehicles+" и транспорта c ограничениями по весу " +VehicleCap + "\n");
                    Stopwatch.Start();
                    s.IntraRouteLocalSearch(Nodes, distanceMatrix);
                    Stopwatch.Stop();
                    s.SolutionPrint("Глобальный алгоритм");
                    Draw.drawRoutes(s, "Глобальный алгоритм");
                    System.out.println("Прошло времени, мс: " + Stopwatch.Result());

                    break;
                case 4:
                    System.out.println("Решение маршрутизации транспортных средств (CVRP) для "+NoOfCustomers+
                            " клинтов "+NoOfVehicles+" и транспорта c ограничениями по весу " +VehicleCap + "\n");
                    Stopwatch.Start();
                    s.TabuSearch(TABU_Horizon, distanceMatrix);
                    Stopwatch.Stop();
                    s.SolutionPrint("Табу алгоритм");
                    Draw.drawRoutes(s, "Табу алгоритм");
                    System.out.println("Прошло времени, мс: " + Stopwatch.Result());

                    break;

                case 5:
                    System.out.print("\u001b[H\u001b[2J");
                    System.out.flush();
                    System.out.println("Привет, пожалуйста, задай количество Агентов в системе: ");
                    Scanner inNumberAgent = new Scanner(System.in);
                    Number_of_Agent = inNumberAgent.nextInt();
                    break;

                case 6:
                    //CerrierList.add(cc.createNewAgent("Company1", "CompanyAgent", new Object[]{"Заказ №1", Integer.toString((int) (Math.random() * 1000.0))}));
                    CerrierList.add(cc.createNewAgent("Company1", "CompanyAgent", new Object[]{"Заказ №2", Integer.toString((int) (Math.random() * 1000.0))}));

                    for (int i = 0; i < Number_of_Agent; i++) {
                        try {

                            CerrierList.add(cc.createNewAgent("Carrier [" + i + "]", "CarrierAgent", new Object[]{}));
                        } catch (StaleProxyException e) {
                            e.printStackTrace();
                        }
                        ;
                    }

                    System.out.print("\u001b[H\u001b[2J");
                    System.out.flush();
                    System.out.println("Агенты успешно зарегистрированы ");

                    break;
                case 7:

                   // AgentSystem.start();
                    for (int i = 0; i < Number_of_Agent+2; i++) {
                        CerrierList.get(i).start();
                    }

                    System.out.print("\u001b[H\u001b[2J");
                    System.out.flush();
                    System.out.println("Торги начались:  ");

                default:
                    System.out.print("\u001b[H\u001b[2J");
                    System.out.flush();
                    System.out.println("Не корректный ввод");
            }
        } while (MenuSystem != 4);

        System.exit(0);
    }

    public static void VRP(){

    }
}