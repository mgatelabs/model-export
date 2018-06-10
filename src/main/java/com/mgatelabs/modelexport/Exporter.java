package com.mgatelabs.modelexport;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Created by @mgatelabs (Michael Fuller) on 6/10/2018.
 */
public class Exporter {

    public static void main(String[] args) {
        if (args.length == 1) {
            final List<String> fileContent = loadFile(new File(args[0]));
            if (fileContent == null || fileContent.isEmpty()) {
                System.out.println("Error - No File Content");
            }
            process(args[0], fileContent);
        } else {
            System.out.println("Please pass in the obj filename");
        }
    }

    public static List<String> loadFile(File f) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(f));
            String line;
            List<String> lines = Lists.newArrayList();
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (Exception ex) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ImmutableList.of();
    }

    public static boolean writeFile(File f, String content) {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            return true;
        } catch (Exception ex) {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void process(String filename, List<String> lines) {

        List<String> allVertexes = Lists.newArrayList();
        List<String> allTexturePoints = Lists.newArrayList();

        List<String> modifiedVertexes = Lists.newArrayList();
        List<String> modifiedTexturePoints = Lists.newArrayList();

        List<Integer> faces = Lists.newArrayList();
        Map<String, Integer> convertedVertexPoints = Maps.newHashMap();

        int indexValue = 0;

        for (String line: lines) {

            if (line.startsWith("v ")) {
                String sub = line.substring(2);

                allVertexes.add(sub.replaceAll(" ", "f,") + "f");

            } else if (line.startsWith("vt ")) {
                String sub = line.substring(3);
                allTexturePoints.add(sub.replaceAll(" ", "f,") + "f");
            } else if (line.startsWith("f ")) {
                String sub = line.substring(2);
                String [] subPieces = sub.split(" ");
                Preconditions.checkArgument(subPieces.length == 3);
                for (int i = 0; i < subPieces.length; i++) {
                    String [] dubPieces = subPieces[i].split("/");
                    Preconditions.checkArgument(dubPieces.length == 3);
                    String vertexPosition = dubPieces[0];
                    String vertexTexturePosition = dubPieces[1];
                    String key = vertexPosition + "-" + vertexTexturePosition;
                    Integer index = convertedVertexPoints.get(key);
                    if (index == null) {
                        int convertedVertexPosition = Integer.parseInt(vertexPosition);
                        int convertedVertexTexturePosition = Integer.parseInt(vertexTexturePosition);

                        modifiedVertexes.add(allVertexes.get(convertedVertexPosition - 1));
                        modifiedTexturePoints.add(allTexturePoints.get(convertedVertexTexturePosition - 1));

                        index = indexValue++;
                        convertedVertexPoints.put(key, index);
                    }

                    faces.add(index);
                }


            }
        }

        StringBuilder javaBuilder = new StringBuilder();
        StringBuilder objBuilder = new StringBuilder();


        javaBuilder.append("/" + "/" + " Vertex Count = " + modifiedVertexes.size()).append("\n");
        javaBuilder.append("float [] vertexes = new float [] {").append("\n");
        for (int i = 0; i < modifiedVertexes.size(); i++) {
            if (i > 0) javaBuilder.append(", ");
            javaBuilder.append(modifiedVertexes.get(i));
        }
        javaBuilder.append("};\n");

        javaBuilder.append("/" + "/" + " UV Count = " + modifiedTexturePoints.size());
        javaBuilder.append("float [] texturePoints = new float [] {").append("\n");
        for (int i = 0; i < modifiedTexturePoints.size(); i++) {
            if (i > 0) javaBuilder.append(", ");
            String [] split = modifiedTexturePoints.get(i).split(",");
            javaBuilder.append(split[0]);
            javaBuilder.append(",");
            javaBuilder.append(1.0 - Float.parseFloat(split[1]) + "f");
        }
        javaBuilder.append("};");


        javaBuilder.append("/" + "/" + " Face Count = " + faces.size() / 3);
        javaBuilder.append("short [] faces = new short [] {");
        for (int i = 0; i < faces.size(); i++) {
            if (i > 0) javaBuilder.append(", ");
            javaBuilder.append(faces.get(i));
        }
        javaBuilder.append("};").append("\n");

        writeFile(new File(filename + ".java"), javaBuilder.toString());

    }

}
