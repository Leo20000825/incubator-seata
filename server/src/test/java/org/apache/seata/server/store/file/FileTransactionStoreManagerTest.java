/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.store.file;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.seata.common.store.SessionMode;
import org.apache.seata.server.session.SessionHolder;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import org.apache.seata.common.util.BufferUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.storage.file.store.FileTransactionStoreManager;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.TransactionStoreManager;
import org.springframework.context.ApplicationContext;

/**
 */
@SpringBootTest
public class FileTransactionStoreManagerTest {

    @BeforeAll
    public static void init(ApplicationContext context){
        SessionHolder.init(SessionMode.FILE);
    }
    @AfterAll
    public static void destroy(){
        SessionHolder.destroy();
    }

    @Test
    public void testBigDataWrite() throws Exception {
        File seataFile = Files.newTemporaryFile();
        FileTransactionStoreManager fileTransactionStoreManager = null;
        try {
            fileTransactionStoreManager = new FileTransactionStoreManager(seataFile.getAbsolutePath(), null);
            BranchSession branchSessionA = Mockito.mock(BranchSession.class);
            GlobalSession global = new GlobalSession();
            Mockito.when(branchSessionA.encode())
                    .thenReturn(createBigBranchSessionData(global, (byte) 'A'));
            Mockito.when(branchSessionA.getApplicationData())
                    .thenReturn(new String(createBigApplicationData((byte) 'A')));
            BranchSession branchSessionB = Mockito.mock(BranchSession.class);
            Mockito.when(branchSessionB.encode())
                    .thenReturn(createBigBranchSessionData(global, (byte) 'B'));
            Mockito.when(branchSessionB.getApplicationData())
                    .thenReturn(new String(createBigApplicationData((byte) 'B')));
            Assertions.assertTrue(fileTransactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSessionA));
            Assertions.assertTrue(fileTransactionStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSessionB));
            List<TransactionWriteStore> list = fileTransactionStoreManager.readWriteStore(2000, false);
            Assertions.assertNotNull(list);
            Assertions.assertEquals(2, list.size());
            BranchSession loadedBranchSessionA = (BranchSession) list.get(0).getSessionRequest();
            Assertions.assertEquals(branchSessionA.getApplicationData(), loadedBranchSessionA.getApplicationData());
            BranchSession loadedBranchSessionB = (BranchSession) list.get(1).getSessionRequest();
            Assertions.assertEquals(branchSessionB.getApplicationData(), loadedBranchSessionB.getApplicationData());
        } finally {
            if (fileTransactionStoreManager != null) {
                fileTransactionStoreManager.shutdown();
            }
            Assertions.assertTrue(seataFile.delete());
        }
    }

    @Test
    public void testFindTimeoutAndSave() throws Exception {
        File seataFile = Files.newTemporaryFile();
        Method findTimeoutAndSaveMethod = FileTransactionStoreManager.class.getDeclaredMethod("findTimeoutAndSave");
        findTimeoutAndSaveMethod.setAccessible(true);
        FileSessionManager sessionManager = null;
        FileTransactionStoreManager fileTransactionStoreManager = null;
        try {
            List<GlobalSession> timeoutSessions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                GlobalSession globalSession = new GlobalSession("", "", "", 60000);
                BranchSession branchSessionA = Mockito.mock(BranchSession.class);
                Mockito.when(branchSessionA.encode())
                        .thenReturn(createBigBranchSessionData(globalSession, (byte) 'A'));
                Mockito.when(branchSessionA.getApplicationData())
                        .thenReturn(new String(createBigApplicationData((byte) 'A')));
                globalSession.addBranch(branchSessionA);
                BranchSession branchSessionB = Mockito.mock(BranchSession.class);
                Mockito.when(branchSessionB.encode())
                        .thenReturn(createBigBranchSessionData(globalSession, (byte) 'B'));
                Mockito.when(branchSessionB.getApplicationData())
                        .thenReturn(new String(createBigApplicationData((byte) 'B')));
                globalSession.addBranch(branchSessionB);
                timeoutSessions.add(globalSession);
            }
            SessionManager sessionManagerMock = Mockito.mock(SessionManager.class);
            Mockito.when(sessionManagerMock.findGlobalSessions(Mockito.any()))
                    .thenReturn(timeoutSessions);
            fileTransactionStoreManager = new FileTransactionStoreManager(
                seataFile.getAbsolutePath(), sessionManagerMock);
            Assertions.assertTrue((boolean) findTimeoutAndSaveMethod.invoke(fileTransactionStoreManager));

            sessionManager = new FileSessionManager(seataFile.getName(), seataFile.getParent());
            sessionManager.reload();
            Collection<GlobalSession> globalSessions = sessionManager.allSessions();
            Assertions.assertNotNull(globalSessions);
            globalSessions.forEach(g -> {
                Assertions.assertNotNull(g);
                List<BranchSession> branches = g.getBranchSessions();
                Assertions.assertEquals(2, branches.size());
                Assertions.assertEquals(new String(createBigApplicationData((byte) 'A')), branches.get(0).getApplicationData());
                Assertions.assertEquals(new String(createBigApplicationData((byte) 'B')), branches.get(1).getApplicationData());
            });
        } finally {
            findTimeoutAndSaveMethod.setAccessible(false);
            if (fileTransactionStoreManager != null) {
                fileTransactionStoreManager.shutdown();
            }
            if (sessionManager != null) {
                sessionManager.destroy();
            }
            Assertions.assertTrue(seataFile.delete());
        }
    }

    private byte[] createBigBranchSessionData(GlobalSession global, byte c) {
        int bufferSize = StoreConfig.getFileWriteBufferCacheSize() // applicationDataBytes
                + 8 // trascationId
                + 8 // branchId
                + 4 // resourceIdBytes.length
                + 4 // lockKeyBytes.length
                + 2 // clientIdBytes.length
                + 4 // applicationDataBytes.length
                + 4 // xidBytes.size
                + 1 // statusCode
                + 1 // lockstatus
                + 1; //branchType
        String xid = global.getXid();
        byte[] xidBytes = null;
        if (xid != null) {
            xidBytes = xid.getBytes();
            bufferSize += xidBytes.length;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        byteBuffer.putLong(global.getTransactionId());
        byteBuffer.putLong(UUIDGenerator.generateUUID());
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putShort((short) 0);
        byte[] applicationDataBytes = createBigApplicationData(c);
        byteBuffer.putInt(applicationDataBytes.length);
        byteBuffer.put(applicationDataBytes);
        if (xidBytes != null) {
            byteBuffer.putInt(xidBytes.length);
            byteBuffer.put(xidBytes);
        } else {
            byteBuffer.putInt(0);
        }
        byteBuffer.put((byte) 0);
        byteBuffer.put((byte) 0);
        byteBuffer.put((byte) 0);
        BufferUtils.flip(byteBuffer);
        byte[] bytes = new byte[byteBuffer.limit()];
        byteBuffer.get(bytes);
        return bytes;
    }

    private byte[] createBigApplicationData(byte c) {
        int applicationDataSize = StoreConfig.getFileWriteBufferCacheSize();
        byte[] applicationDataBytes = new byte[applicationDataSize];
        for (int i = 0; i < applicationDataSize; i++) {
            applicationDataBytes[i] = c;
        }
        return applicationDataBytes;
    }
}
