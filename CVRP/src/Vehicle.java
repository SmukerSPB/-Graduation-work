import java.util.ArrayList;

public class Vehicle {
    public int VehId;
    public ArrayList<Node> Route = new ArrayList<Node>();
    public int capacity;
    public int load;
    public int CurLoc;
    public boolean Closed;

    /**
     * Конструктор по созданию транспортного средства
     */
    public Vehicle(int id, int cap)
    {
        this.VehId = id;
        this.capacity = cap;
        this.load = 0;
        this.CurLoc = 0;
        this.Closed = false;
        this.Route.clear();
    }

    /**
     * Добавление нового клиента на карту
     */
    public void AddNode(Node Customer )
    {
        Route.add(Customer);
        this.load +=  Customer.demand;
        this.CurLoc = Customer.NodeId;
    }

    /**
     * Проверяем, присутствуем нарушение емкости
     */
    public boolean CheckIfFits(int dem)
    {
        return ((load + dem <= capacity));
    }
}
