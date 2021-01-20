package com.github.harbby.ashtarte;

import com.github.harbby.ashtarte.api.Partition;
import com.github.harbby.ashtarte.api.Stage;
import com.github.harbby.ashtarte.operator.ShuffleMapOperator;

public class ShuffleMapStage
        extends Stage
{
    public ShuffleMapStage(ShuffleMapOperator<?, ?> operator, int jobId, int stageId)
    {
        super(operator, jobId, stageId);
    }

    @Override
    public void compute(Partition split, TaskContext taskContext)
    {
        getFinalOperator().computeOrCache(split, taskContext);
    }
}
