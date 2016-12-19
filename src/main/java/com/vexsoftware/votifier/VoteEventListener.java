package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener to dispatch the vote to all listeners.
 *
 * @author Blake Beaupain
 * @author Kramer Campbell
 * @author https://github.com/vaess
 */
public class VoteEventListener implements Listener {

    @EventHandler(priority= EventPriority.NORMAL)
    public void onVotifierEvent(VotifierEvent event) {
        if(event.isCancelled())
            return;

        for (com.vexsoftware.votifier.model.VoteListener listener : Votifier.getInstance().getListeners()) {
            try {
                listener.voteMade(event.getVote());
            } catch (Exception ex) {
                String vlName = listener.getClass().getSimpleName();
                Logger.getLogger("Votifier").log(Level.WARNING,"Exception caught while sending the vote notification to the '" + vlName + "' listener", ex);
            }
        }
    }
}
