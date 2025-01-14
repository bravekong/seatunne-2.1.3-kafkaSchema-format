/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.core.spark.command;

import org.apache.seatunnel.apis.base.api.BaseSink;
import org.apache.seatunnel.apis.base.api.BaseSource;
import org.apache.seatunnel.apis.base.api.BaseTransform;
import org.apache.seatunnel.apis.base.env.Execution;
import org.apache.seatunnel.core.base.command.BaseTaskExecuteCommand;
import org.apache.seatunnel.core.base.config.ConfigBuilder;
import org.apache.seatunnel.core.base.config.EngineType;
import org.apache.seatunnel.core.base.config.ExecutionFactory;
import org.apache.seatunnel.core.base.exception.CommandExecuteException;
import org.apache.seatunnel.core.base.utils.FileUtils;
import org.apache.seatunnel.core.spark.args.SparkCommandArgs;
import org.apache.seatunnel.core.spark.config.SparkExecutionContext;
import org.apache.seatunnel.spark.SparkEnvironment;

import org.apache.seatunnel.shade.com.typesafe.config.Config;

import java.nio.file.Path;
import java.util.List;

public class SparkTaskExecuteCommand extends BaseTaskExecuteCommand<SparkCommandArgs, SparkEnvironment> {

    private final SparkCommandArgs sparkCommandArgs;

    public SparkTaskExecuteCommand(SparkCommandArgs sparkCommandArgs) {
        this.sparkCommandArgs = sparkCommandArgs;
    }

    @Override
    public void execute() throws CommandExecuteException {
        EngineType engine = sparkCommandArgs.getEngineType();
        Path confFile = FileUtils.getConfigPath(sparkCommandArgs);

        Config config = new ConfigBuilder(confFile).getConfig();
        SparkExecutionContext executionContext = new SparkExecutionContext(config, engine);

        List<BaseSource<SparkEnvironment>> sources = executionContext.getSources();
        List<BaseTransform<SparkEnvironment>> transforms = executionContext.getTransforms();
        List<BaseSink<SparkEnvironment>> sinks = executionContext.getSinks();

        baseCheckConfig(sources, transforms, sinks);
        showAsciiLogo();

        try (Execution<
            BaseSource<SparkEnvironment>,
            BaseTransform<SparkEnvironment>,
            BaseSink<SparkEnvironment>, SparkEnvironment> execution = new ExecutionFactory<>(executionContext).createExecution()) {
            prepare(executionContext.getEnvironment(), sources, transforms, sinks);
            execution.start(sources, transforms, sinks);
        } catch (Exception e) {
            throw new CommandExecuteException("Execute Spark task error", e);
        } finally {
            close(sources, transforms, sinks);
        }
    }

}
