import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.Collectors;

//Class created for all seats in theatre
class Seat {
    private final int seatNo;
    private boolean reserved;

    //Set seat and reserved status
    public Seat(int seatNo) {
        this.seatNo = seatNo;
        this.reserved = false;
    }

    //Seats that are reserved
    public synchronized boolean reserve() {
        if (!reserved) {
            reserved = true;
            return true;
        }
        return false;
    }

    //Seats that are not reserved
    public synchronized void unreserve() {
        reserved = false;
    }

    public int getSeatNo() {
        return seatNo;
    }
}

//Class created for each theatre
class Theatre {
    private final int theatreNo;
    private final List<Seat> seats;

    //Method to assign seats in theatre
    public Theatre(int theatreNo) {
        this.theatreNo = theatreNo;
        this.seats = new ArrayList<>();
        //Limit the number of seats to be maximum 20
        for (int i = 1; i <= 20; i++) {
            seats.add(new Seat(i));
        }
    }

    //Method to assign reserved seats in theatre
    public synchronized List<Seat> getReservedSeats(int count) {
        List<Seat> reservedSeats = new ArrayList<>();
        for (Seat seat : seats) {
            if (!seat.reserve()) continue;
            reservedSeats.add(seat);
            if (reservedSeats.size() == count) break;
        }

        //Check number of successfully reserved seats
        if (reservedSeats.size() < count) {
            //If seats not reserved, change status to unreserved
            for (Seat seat : reservedSeats) {
                seat.unreserve();
            }
            //returns empty list for unsuccessful reservation
            return Collections.emptyList();
        }
        //returns successfully reserved seats
        return reservedSeats;
    }

    public int getTheatreNumber() {
        return theatreNo;
    }
}

//Class created for customers
class Customer implements Runnable {
    private final int customerId;

    private final List<Theatre> theatres;
    private final List<String> successBooking; //Successful bookings by customers
    private final List<Integer> failBooking; //Failed bookings by customers

    //Setters
    public Customer(int customerId, List<Theatre> theatres, List<String> successBooking, List<Integer> failBooking) {
        this.customerId = customerId;
        this.theatres = theatres;
        this.successBooking = successBooking;
        this.failBooking = failBooking;
    }

    @Override
    public void run() {
        Random rand = new Random();
        //Random to determine the number of seats being booked by customer
        Theatre selectedTheatre = theatres.get(rand.nextInt(theatres.size()));
        //Customer can book up 1-3 seats
        int seatCount = rand.nextInt(3) + 1;

        //Store reserved seats in list
        List<Seat> reservedSeats = selectedTheatre.getReservedSeats(seatCount);

        //Customers with no reserved seats added to failed bookings
        if (reservedSeats.isEmpty()) {
            synchronized (failBooking) {
                failBooking.add(customerId);
            }
            return;
        }

        try {
            //Add delay to simulate confirmation of reservation
            Thread.sleep(500 + rand.nextInt(501)); //500ms to 1000ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        //Adding successful reservation into successful bookings
        String result = String.format("Customer %2d successfully reserved Seat No. %s in Theatre %d",
                customerId,
                reservedSeats.stream().map(seat -> Integer.toString(seat.getSeatNo())).collect(Collectors.joining(", ")),
                selectedTheatre.getTheatreNumber());

        synchronized (successBooking) {
            successBooking.add(result);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        //Set the number of total number of theatres to 3
        List<Theatre> theatres = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            theatres.add(new Theatre(i));
        }

        List<String> successBooking = Collections.synchronizedList(new ArrayList<>());
        List<Integer> failBooking = Collections.synchronizedList(new ArrayList<>());

        //Create 100 threads that resembles 100 customers
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        for (int i = 1; i <= 100; i++) {
            executorService.execute(new Customer(i, theatres, successBooking, failBooking));
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        //Print the customers and their reserved seats
        successBooking.forEach(System.out::println);

        //Print the customers who could not reserve seats
        if (!failBooking.isEmpty()) {
            System.out.print("Customers unable to reserve seats: ");
            System.out.println(failBooking.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
    }
}
