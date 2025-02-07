package ru.hollowhorizon.hc.common.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import ru.hollowhorizon.hc.common.network.messages.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NetworkHandler {
    public static final String MESSAGE_PROTOCOL_VERSION = "1.0";
    public static final ResourceLocation HOLLOW_CORE_CHANNEL = new ResourceLocation("hc", "hollow_core_channel");
    public static SimpleChannel HollowCoreChannel;
    public static int PACKET_INDEX = 0;
    public static final List<Runnable> PACKET_TASKS = new ArrayList<>();

    public static <MSG> void sendMessageToClient(MSG messageToClient, PlayerEntity player) {
        HollowCoreChannel.sendTo(messageToClient, ((ServerPlayerEntity) player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <MSG> void sendMessageToServer(MSG messageToServer) {
        HollowCoreChannel.sendToServer(messageToServer);
    }

    public static void register() {
        if (HollowCoreChannel == null) {
            HollowCoreChannel = NetworkRegistry.newSimpleChannel(HOLLOW_CORE_CHANNEL, () -> MESSAGE_PROTOCOL_VERSION,
                    MESSAGE_PROTOCOL_VERSION::equals,
                    MESSAGE_PROTOCOL_VERSION::equals
            );
        }

        PACKET_TASKS.forEach(Runnable::run);
    }
}
