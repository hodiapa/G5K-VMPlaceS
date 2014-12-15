package Common;/* ============================================================
 * Discovery Project - AkkaArc
 * http://beyondtheclouds.github.io/
 * ============================================================
 * Copyright 2013 Discovery Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============================================================ */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellAdaptor {

    public void executeShellAsynchronously(String cmd) throws IOException {

        String[] shellCmd = {
                "/bin/bash",
                "-c",
                cmd
        };

        System.out.println(cmd);

        Runtime runtime = Runtime.getRuntime();
        final Process p = runtime.exec(shellCmd);
    }

    public String executeShellSynchronously(String cmd) throws IOException {

        String[] shellCmd = {
                "/bin/bash",
                "-c",
                cmd
        };

        System.out.println(cmd);

        Runtime runtime = Runtime.getRuntime();
        final Process p = runtime.exec(shellCmd);

        String result = "";

        BufferedReader in =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {

            if(!result.equals("")) {
                result += "\n";
            }

            result += inputLine;
        }
        in.close();


        return result;
    }
}
