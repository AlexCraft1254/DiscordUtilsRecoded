package md.mirrerror.discordutils.discord;

import md.mirrerror.discordutils.Main;
import md.mirrerror.discordutils.data.DataManager;
import md.mirrerror.discordutils.discord.cache.DiscordUtilsUsersCacheManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.bukkit.OfflinePlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiscordUtilsUser {

    private DataManager dataManager = Main.getInstance().getDataManager();

    private User user;
    private OfflinePlayer offlinePlayer;
    private boolean hasSecondFactor;

    public DiscordUtilsUser(OfflinePlayer offlinePlayer, User user, boolean hasSecondFactor) {
        this.offlinePlayer = offlinePlayer;
        this.user = user;
        this.hasSecondFactor = hasSecondFactor;
    }

    public void setUser(User user) {
        this.user = user;
        dataManager.setDiscordUserId(offlinePlayer.getUniqueId(), user.getIdLong());
        DiscordUtilsUsersCacheManager.addToCache(this);
    }

    public void setSecondFactor(boolean secondFactor) {
        this.hasSecondFactor = secondFactor;
        dataManager.setSecondFactor(offlinePlayer.getUniqueId(), secondFactor);
        DiscordUtilsUsersCacheManager.addToCache(this);
    }

    public boolean hasSecondFactor() {
        return hasSecondFactor;
    }

    public boolean isLinked() {
        return offlinePlayer != null && user != null;
    }

    public boolean isAdmin(Guild guild) {
        for(long roleId : Main.getInstance().getBot().getAdminRoles()) {
            for(Role role : guild.getMemberById(user.getIdLong()).getRoles()) {
                if(role.getIdLong() == roleId) return true;
            }
        }
        return false;
    }

    public void synchronizeRoles(Guild guild) {
        if(!isLinked()) return;
        Set<Long> assignedRoles = new HashSet<>();
        Member member = guild.getMemberById(user.getIdLong());

        if(Main.getInstance().getConfigManager().getBotSettings().getFileConfiguration().getBoolean("RolesSynchronization.AssignOnlyPrimaryGroup")) {
            String primaryGroup = Main.getInstance().getPermissionsIntegration().getHighestUserGroup(offlinePlayer);

            for(long roleId : Main.getInstance().getBot().getGroupRoles().keySet()) {
                if(Main.getInstance().getBot().getGroupRoles().get(roleId).contains(primaryGroup)) {
                    try {
                        Role role = guild.getRoleById(roleId);
                        guild.addRoleToMember(member, role).queue();
                        assignedRoles.add(roleId);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } else {
            List<String> playerGroups = Main.getInstance().getPermissionsIntegration().getUserGroups(offlinePlayer);

            for(long roleId : Main.getInstance().getBot().getGroupRoles().keySet()) {
                if(Main.getInstance().getBot().getGroupRoles().get(roleId).stream().distinct().anyMatch(playerGroups::contains)) {
                    try {
                        Role role = guild.getRoleById(roleId);
                        guild.addRoleToMember(member, role).queue();
                        assignedRoles.add(roleId);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        for(long roleId : Main.getInstance().getBot().getGroupRoles().keySet()) {
            if(!assignedRoles.contains(roleId)) {
                try {
                    guild.removeRoleFromMember(member, guild.getRoleById(roleId)).queue();
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void synchronizeNickname(Guild guild) {
        if(!isLinked()) return;
        String format = Main.getInstance().getPapiManager().setPlaceholders(offlinePlayer,
                Main.getInstance().getConfigManager().getBotSettings().getFileConfiguration().getString("NamesSynchronization.NamesSyncFormat")
                        .replace("%player%", offlinePlayer.getName()));
        try {
            guild.modifyNickname(guild.getMemberById(user.getIdLong()), format).queue();
        } catch (IllegalArgumentException | HierarchyException ignored) {}
    }

    public void unregister() {
        this.user = null;
        this.hasSecondFactor = false;
        Main.getInstance().getDataManager().unregisterUser(offlinePlayer.getUniqueId());
        DiscordUtilsUsersCacheManager.addToCache(this);
    }

    public User getUser() {
        return user;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

}
