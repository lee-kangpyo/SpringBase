package com.akmz.springBase.attach.service;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Component;

@Component
public class FtpClientFactory {
    public FTPClient createClient() {
        return new FTPClient();
    }
}
