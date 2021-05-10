package carpet.helpers;

import carpet.CarpetServer;
import carpet.utils.extensions.ExtendedScore;
import java.util.Collection;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;

/**
 * Class created for
 */
public class ScoreboardDelta {

    public static void update() {
        MinecraftServer server = CarpetServer.getMinecraftServer();
        for(int i = 0; i < 2; i++) {
            Scoreboard scoreboard = server.getWorldById(0).getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(i);
            Collection<ScoreboardPlayerScore> list = scoreboard.getAllPlayerScores(objective);

            for(ScoreboardPlayerScore s : list){
                ((ExtendedScore) s).computeScoreDelta();
                s.getScoreboard().updateScore(s);
                if(((ExtendedScore) s).getScorePointsDelta() == 0){
                    s.getScoreboard().updatePlayerScore(s.getPlayerName());
                }
            }
        }
    }

    public static void resetScoreboardDelta() {
        MinecraftServer server = CarpetServer.getMinecraftServer();
        for(int i = 0; i < 2; i++) {
            Scoreboard scoreboard = server.getWorldById(0).getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(i);
            Collection<ScoreboardPlayerScore> list = scoreboard.getAllPlayerScores(objective);

            for(ScoreboardPlayerScore s : list){
                ((ExtendedScore) s).computeScoreDelta();
                s.getScoreboard().updateScore(s);
            }
        }
    }
}
