/*
 * Copyright (C) 2018 The Astarte Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.harbby.astarte.core.api;

import com.github.harbby.astarte.core.Partitioner;
import com.github.harbby.astarte.core.api.function.Comparator;
import com.github.harbby.astarte.core.operator.SortShuffleWriter;
import com.github.harbby.gadtry.base.Serializables;
import com.github.harbby.gadtry.collection.tuple.Tuple2;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.github.harbby.astarte.core.runtime.ShuffleManagerService.getShuffleWorkDir;

public interface ShuffleWriter<K, V>
        extends Closeable
{
    public File getDataFile(int shuffleId, int mapId, int reduceId);

    public void write(Iterator<? extends Tuple2<K, V>> iterator)
            throws IOException;

    public static <K, V> ShuffleWriter<K, V> createShuffleWriter(
            String executorUUID,
            int jobId,
            int shuffleId,
            int mapId,
            Partitioner partitioner,
            Comparator<K> ordering)
    {
        if (ordering != null) {
            return new SortShuffleWriter<>(executorUUID, jobId, shuffleId, mapId, ordering, partitioner);
        }
        return new HashShuffleWriter<>(executorUUID, jobId, shuffleId, mapId, partitioner);
    }

    public static class HashShuffleWriter<K, V>
            implements ShuffleWriter<K, V>
    {
        private final String executorUUID;
        private final int shuffleId;
        private final int mapId;
        private final int jobId;
        private final Partitioner partitioner;
        //todo: use array index, not hash
        private final Map<Integer, DataOutputStream> outputStreamMap = new HashMap<>();

        public HashShuffleWriter(
                String executorUUID,
                int jobId,
                int shuffleId,
                int mapId,
                Partitioner partitioner)
        {
            this.executorUUID = executorUUID;
            this.jobId = jobId;
            this.shuffleId = shuffleId;
            this.mapId = mapId;
            this.partitioner = partitioner;
        }

        protected void write(int reduceId, Serializable value)
                throws IOException
        {
            DataOutputStream dataOutputStream = outputStreamMap.get(reduceId);
            if (dataOutputStream == null) {
                File file = this.getDataFile(shuffleId, mapId, reduceId);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                dataOutputStream = new DataOutputStream(new FileOutputStream(this.getDataFile(shuffleId, mapId, reduceId), false));
                outputStreamMap.put(reduceId, dataOutputStream);
            }

            byte[] bytes = Serializables.serialize(value);
            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);
        }

        @Override
        public void write(Iterator<? extends Tuple2<K, V>> iterator)
                throws IOException
        {
            while (iterator.hasNext()) {
                Tuple2<K, V> kv = iterator.next();
                int reduceId = partitioner.getPartition(kv.f1());
                this.write(reduceId, kv);
            }
        }

        @Override
        public File getDataFile(int shuffleId, int mapId, int reduceId)
        {
            // spark path /tmp/blockmgr-0b4744ba-bffa-420d-accb-fbc475da7a9d/27/shuffle_101_201_0.data
            String fileName = "shuffle_" + shuffleId + "_" + mapId + "_" + reduceId + ".data";
            File shuffleWorkDir = getShuffleWorkDir(executorUUID);
            return new File(shuffleWorkDir, String.format("%s/%s", jobId, fileName));
        }

        @Override
        public void close()
                throws IOException
        {
            for (DataOutputStream dataOutputStream : outputStreamMap.values()) {
                dataOutputStream.close();
            }
        }
    }
}
