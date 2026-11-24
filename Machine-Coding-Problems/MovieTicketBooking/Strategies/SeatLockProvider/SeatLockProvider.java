package MovieTicketBooking.Strategies.SeatLockProvider;

import MovieTicketBooking.Core.Seat;
import MovieTicketBooking.Core.SeatLock;
import MovieTicketBooking.Core.User;

public interface SeatLockProvider
{
    public SeatLock lockSeat(Seat seat, User user);
    public void unlockSeat(SeatLock seatLock);
}
