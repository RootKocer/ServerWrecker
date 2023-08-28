/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import net.pistonmaster.serverwrecker.grpc.generated.command.CommandServiceGrpc;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogsServiceGrpc;

import java.util.concurrent.TimeUnit;

public class RPCClient {
    private final ManagedChannel channel;
    @Getter
    private final LogsServiceGrpc.LogsServiceStub logStub;
    @Getter
    private final CommandServiceGrpc.CommandServiceStub commandStub;
    @Getter
    private final CommandServiceGrpc.CommandServiceBlockingStub commandStubBlocking;

    public RPCClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC client since JVM is shutting down");
            try {
                shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** client shut down");
        }));
    }

    public RPCClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        logStub = LogsServiceGrpc.newStub(channel).withCompression("gzip");
        commandStub = CommandServiceGrpc.newStub(channel).withCompression("gzip");
        commandStubBlocking = CommandServiceGrpc.newBlockingStub(channel).withCompression("gzip");
    }

    public void shutdown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
}
