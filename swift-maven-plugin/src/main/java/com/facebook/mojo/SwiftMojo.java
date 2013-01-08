/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.mojo;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import com.facebook.swift.generator.SwiftGenerator;
import com.facebook.swift.generator.SwiftGeneratorConfig;
import com.google.common.base.Throwables;
import com.pyx4j.log4j.MavenLogAppender;

/**
 * Process IDL files and generates source code from the IDL files.
 *
 * @requiresProject true
 * @goal generate
 * @phase generate-sources
 * @requiresProject true
 */
public class SwiftMojo extends AbstractMojo
{
    private static final Logger LOG = Logger.getLogger(SwiftMojo.class);

    /**
     * Skip the plugin execution.
     *
     * @parameter default-value="false"
     */
    private boolean skip = false;

    /**
     * Override java package for the generated classes. If unset, the java
     * namespace from the IDL files is used. If a value is set here, the java package
     * definition from the IDL files is ignored.
     *
     * @parameter
     */
    private String overridePackage = null;

    /**
     * Give a default Java package for generated classes if the IDL files do not
     * contain a java namespace definition. This package is only used if the IDL files
     * do not contain a java namespace definition.
     *
     * @parameter
     */
    private String defaultPackage = null;

    /**
     * IDL files to process.
     *
     * @parameter
     * @required
     */
    private FileSet idlFiles;

    /**
     * Set the Output folder for generated code.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/swift"
     * @required
     */
    private File outputFolder = null;

    /**
     * Generate code for included IDL files. If true, generate Java code for all IDL files
     * that are listed in the idlFiles set and all IDL files loaded through include statements.
     * Default is false (generate only code for explicitly listed IDL files).
     *
     * @parameter default-value="false"
     */
    private boolean generateIncludedCode = false;

    /**
     * Add {@link org.apache.thrift.TException} to each method signature. This exception is thrown
     * when a thrift internal error occurs.
     *
     * @parameter default-value="true"
     */
    private boolean addThriftExceptions = true;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException
    {
        MavenLogAppender.startPluginLog(this);

        try {
            if (!skip) {

                final File inputFolder = new File(idlFiles.getDirectory());

                @SuppressWarnings("unchecked")
                List<File> files = FileUtils.getFiles(inputFolder,
                                                      StringUtils.join(idlFiles.getIncludes(), ','),
                                                      StringUtils.join(idlFiles.getExcludes(), ','));
                final SwiftGeneratorConfig config = SwiftGeneratorConfig.builder()
                    .inputFolder(inputFolder)
                    .addInputFiles(files)
                    .outputFolder(outputFolder)
                    .overridePackage(overridePackage)
                    .defaultPackage(defaultPackage)
                    .addThriftExceptions(addThriftExceptions)
                    .generateIncludedCode(generateIncludedCode)
                    .build();

                final SwiftGenerator generator = new SwiftGenerator(config);
                generator.parse();

                project.addCompileSourceRoot(outputFolder.getPath());
            }
        }
        catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, MojoExecutionException.class);
            Throwables.propagateIfInstanceOf(e, MojoFailureException.class);

            LOG.error(String.format("While executing Mojo %s", this.getClass().getSimpleName()), e);
            throw new MojoExecutionException("Failure:" ,e);
        }
        finally {
            MavenLogAppender.endPluginLog(this);
        }
    }
}