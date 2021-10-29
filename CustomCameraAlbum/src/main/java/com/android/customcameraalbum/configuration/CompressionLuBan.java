package com.android.customcameraalbum.configuration;

import android.content.Context;

import com.android.customcameraalbum.listener.CompressionInterface;

import java.io.File;
import java.io.IOException;

import top.zibin.luban.Luban;


/**
 * luban压缩
 *
 * @author zhongjh
 * @date 2021/9/26
 */
public class CompressionLuBan implements CompressionInterface {

    @Override
    public File compressionFile(Context context, File file) throws IOException {
        return Luban.with(context).load(file).get().get(0);
    }

}
