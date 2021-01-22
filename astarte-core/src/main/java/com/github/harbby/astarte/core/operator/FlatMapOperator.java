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

package com.github.harbby.astarte.core.operator;

import com.github.harbby.astarte.core.TaskContext;
import com.github.harbby.astarte.core.api.Partition;
import com.github.harbby.astarte.core.api.function.Mapper;
import com.github.harbby.gadtry.base.Iterators;

import java.util.Iterator;
import java.util.stream.Stream;

public class FlatMapOperator<I, O>
        extends Operator<O>
{
    private final Mapper<I, O[]> flatMapper;
    private final Operator<I> dataSet;

    protected FlatMapOperator(Operator<I> dataSet, Mapper<I, O[]> flatMapper)
    {
        super(dataSet);
        this.flatMapper = flatMapper;
        this.dataSet = unboxing(dataSet);
    }

    @Override
    public Iterator<O> compute(Partition partition, TaskContext taskContext)
    {
        return Iterators.concat(Iterators.map(dataSet.computeOrCache(partition, taskContext),
                row -> Stream.of(flatMapper.map(row)).iterator()));
    }
}
