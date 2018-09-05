package com.moizahmed.cameraapp;


import android.databinding.DataBindingUtil;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.moizahmed.cameraapp.databinding.ActivityPreviewGifBinding;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

public class PreviewGIF extends AppCompatActivity {

    ActivityPreviewGifBinding binding;
    String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_preview_gif);
        showGIF();
        initListener();
    }

    private void initListener() {
        binding.save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.layoutDetail.getVisibility() == View.VISIBLE) {
                    if (validateForm()) {
                        renameSaveFile();
                        saveDataIntoCSV();

                    }

                } else {
                    binding.gifHolder.setVisibility(View.GONE);
                    binding.layoutDetail.setVisibility(View.VISIBLE);
                    binding.reset.setVisibility(View.GONE);
                }
            }
        });


        binding.reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onBackPressed();
            }
        });
    }

    private boolean validateForm() {
        boolean allClear = true;

        if (binding.name.getText().toString().trim().equals("")) {
            allClear = false;
            binding.name.setError("This field is empty");
        }

        if (binding.email.getText().toString().trim().equals("")) {
            allClear = false;
            binding.email.setError("This field is empty");
        } else if (!isValidEmail(binding.email.getText().toString())) {
            allClear = false;
            binding.email.setError("The email Address is not valid");
        }

        return allClear;
    }

    private void deleteGIFFile() {
        File folder = new File("/sdcard/LUX/");
        File oldfile = new File(folder, fileName);
        oldfile.delete();
    }

    private void renameSaveFile() {
        File folder = new File("/sdcard/LUX/");
        File oldfile = new File(folder, fileName);
        File newfile = new File(folder, binding.email.getText().toString() + ".gif");
        if (oldfile.renameTo(newfile)) {
            Toast.makeText(PreviewGIF.this, "File Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showGIF() {
        fileName = getIntent().getStringExtra("fileName");
        File folder = new File("/sdcard/LUX/");
        File output = new File(folder, fileName);

        Glide.with(this)
                .load(output)
                .into(binding.gifHolder);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (binding.layoutDetail.getVisibility() == View.VISIBLE) {
            binding.gifHolder.setVisibility(View.VISIBLE);
            binding.layoutDetail.setVisibility(View.GONE);
            binding.reset.setVisibility(View.VISIBLE);
        } else {
            deleteGIFFile();
            super.onBackPressed();
        }
    }

    private boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }


    private void saveDataIntoCSV() {
        File folder = new File("/sdcard/LUX/");
        File file = new File(folder, "results.csv");
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        try {
            CsvWriter csvWriter = new CsvWriter();
      /*      CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8);
            csvAppender.appendLine(binding.name.getText().toString(), binding.email.getText().toString());
            csvAppender.endLine();*/

            Writer writer = new FileWriter(file,true);
            Collection<String[]> data = new ArrayList<>();
            data.add(new String[]{binding.name.getText().toString(), binding.email.getText().toString()});
            csvWriter.write(writer,data );
            //writer.append(binding.name.getText().toString()+":"+binding.email.getText().toString()+"\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}