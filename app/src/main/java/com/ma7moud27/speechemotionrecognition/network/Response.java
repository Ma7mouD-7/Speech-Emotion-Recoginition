package com.ma7moud27.speechemotionrecognition.network;

public class Response {
    String filename ;
    String filepath ;
    String prediction;

    public Response(String filename, String filepath, String prediction) {
        this.filename = filename;
        this.filepath = filepath;
        this.prediction = prediction;
    }

    public String getFile_name() {
        return filename;
    }

    public String getFile_path() {
        return filepath;
    }

    public void setFile_name(String filename) {
        this.filename = filename;
    }

    public void setFile_path(String filepath) {
        this.filepath = filepath;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPrediction() {
        return prediction;
    }
}
