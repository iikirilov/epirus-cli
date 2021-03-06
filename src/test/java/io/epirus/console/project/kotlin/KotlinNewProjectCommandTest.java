/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.epirus.console.project.kotlin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.epirus.console.Epirus;
import io.epirus.console.project.NewProjectCommand;
import io.epirus.console.project.utils.ClassExecutor;
import io.epirus.console.project.utils.Folders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.WalletUtils;

import static java.io.File.separator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KotlinNewProjectCommandTest extends ClassExecutor {
    private static String tempDirPath;

    @BeforeAll
    static void setUpStreams() {
        tempDirPath = Folders.tempBuildFolder().getAbsolutePath();
    }

    @Test
    @Order(1)
    public void testWhenCorrectArgsArePassedProjectStructureCreated() {
        final String[] args = {"-p=org.com", "-n=Test", "-o=" + tempDirPath};
        final NewProjectCommand newProjectCommand = new NewProjectCommand();
        new CommandLine(newProjectCommand).parseArgs(args);
        assert newProjectCommand.packageName.equals("org.com");
        assert newProjectCommand.projectName.equals("Test");
        assert newProjectCommand.outputDir.equals(tempDirPath);
    }

    @Test
    @Order(2)
    public void testWithPicoCliWhenArgumentsAreCorrect() throws IOException, InterruptedException {
        final String[] args = {
            "new", "--kotlin", "-p", "org.com", "-n", "Test", "-o" + tempDirPath
        };
        int exitCode =
                executeClassAsSubProcessAndReturnProcess(
                                Epirus.class, Collections.emptyList(), Arrays.asList(args), true)
                        .inheritIO()
                        .start()
                        .waitFor();
        assertEquals(0, exitCode);
    }

    @Test
    @Order(3)
    public void verifyThatTestsAreGenerated() {
        final File pathToTests =
                new File(
                        String.join(
                                separator,
                                tempDirPath,
                                "Test",
                                "src",
                                "test",
                                "kotlin",
                                "org",
                                "com",
                                "generated",
                                "contracts",
                                "HelloWorldTest.kt"));
        assertTrue(pathToTests.exists());
    }

    @Test
    public void testWithPicoCliWhenArgumentsAreEmpty() throws IOException, InterruptedException {
        final String[] args = {"new", "--kotlin", "-n=", "-p="};
        ProcessBuilder pb =
                executeClassAsSubProcessAndReturnProcess(
                        Epirus.class, Collections.emptyList(), Arrays.asList(args), false);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            assertEquals(
                    1L,
                    reader.lines()
                            .filter(
                                    l ->
                                            l.contains(
                                                    "Please make sure the required parameters are not empty."))
                            .count());
        }
        process.waitFor();
    }

    @Test
    public void testWhenInteractiveAndArgumentsAreCorrect()
            throws IOException, InterruptedException, NoSuchAlgorithmException,
                    NoSuchProviderException, InvalidAlgorithmParameterException, CipherException {
        final String[] args = {"new", "--kotlin"};
        Process process =
                executeClassAsSubProcessAndReturnProcess(
                                Epirus.class, Collections.emptyList(), Arrays.asList(args), true)
                        .start();
        BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        final String walletName = WalletUtils.generateNewWalletFile("", new File(tempDirPath));
        final String walletPath = tempDirPath + separator + walletName;
        writer.write("Test1", 0, "Test1".length());
        writer.newLine();
        writer.write("org.com", 0, "org.com".length());
        writer.newLine();
        writer.write("n", 0, "n".length());
        writer.newLine();
        writer.write(walletPath, 0, walletPath.length());
        writer.newLine();
        writer.write(" ", 0, " ".length());
        writer.newLine();
        writer.write(tempDirPath, 0, tempDirPath.length());
        writer.newLine();
        writer.close();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            List<String> stringList = reader.lines().collect(Collectors.toList());
            stringList.forEach(string -> System.out.println(string + "\n"));
        }

        process.waitFor();
        assertEquals(0, process.exitValue());
    }
}
