package com.example.player;




import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity implements Runnable {
    //private MediaPlayer mediaPlayer = new MediaPlayer();
    private MediaPlayer mediaPlayer = null;
    private TextView seekBarHint;
    private SeekBar seekBar;
    private FloatingActionButton fabPlayPause;
    private boolean wasPlaying = false;
    private String metaData;
    private TextView metaDataAudio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabPlayPause = findViewById(R.id.fabPlayPause); //поле PlayPause
        seekBarHint = findViewById(R.id.seekBarHint); //поле BarHint
        seekBar = findViewById(R.id.seekBar);//поле seekBar
        metaDataAudio = findViewById(R.id.metaDataAudio);//поле DataAudio


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) { //OnProgressChanged
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBarHint.setVisibility(View.VISIBLE); //установка видимости
                int timeTrack = (int) Math.ceil(progress / 1000f); //Перевод времени из милисекунд в секунды
                // Более удобный вывод seekBarHint
                LocalTime timeOfDay = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    DateTimeFormatter dtf;
                    dtf = DateTimeFormatter.ofPattern("mm:ss");
                    timeOfDay = LocalTime.parse(LocalTime.ofSecondOfDay(timeTrack).format(dtf));
                }
                String time = timeOfDay.toString();

                seekBarHint.setText(time);

                //передвижение бегунка по линии
                double percentTrack = progress / (double) seekBar.getMax(); //получаем процент прогресса
                //seekBar.getX() - начало seekBar по оси Х
                //seekBar.getWidth() - ширна контейнера
                //0.92 - поправочный коэффициент, так как seekBar не всю ширину в контейнере
                seekBarHint.setX(seekBar.getX() + Math.round(seekBar.getWidth() * percentTrack * 0.92));

                if (progress > 0 && mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    seekBar.setProgress(currentPosition);
                }
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarHint.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });

        fabPlayPause.setOnClickListener(listener);


    }

    private  View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playSong();
        }
    };


    //Метод воспроизведения
    public void playSong() {

        try {

            if (!wasPlaying || mediaPlayer == null) {
                //if (mediaPlayer == null)
                mediaPlayer = new MediaPlayer();

                //Назначение кнопке картинку паузы
                fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause));

                //Открытие файла
                AssetFileDescriptor descriptor = getAssets().openFd("app_src_main_res_raw_flight_of_the_bumblebee.mp3");
                mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());

                //Получение метаданных файла
                MediaMetadataRetriever mediaMetadata = new MediaMetadataRetriever();
                mediaMetadata.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());// Загрузка файла в плейер
                metaData = mediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE); //Получаем титл файла

                //Получаем автора файла
                String authorData = mediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
                if (authorData != null)  metaData += "\n" + authorData;
                descriptor.close(); //Закрытие файла
                mediaMetadata.release(); //Освобожение ресурса

                metaDataAudio.setText(metaData); //Установка текста название трека

                mediaPlayer.prepare(); //Подготовка плеера к проигрованию
                mediaPlayer.setLooping(false);
                seekBar.setMax(mediaPlayer.getDuration()); //Установка максимальной длинны трека в прогрессбар
                mediaPlayer.start(); //Старт потока
                new Thread(this).start(); //Запуск дополнительного потока

                wasPlaying = true;
            }else {


                boolean isPlay = mediaPlayer.isPlaying();

                if (mediaPlayer != null && isPlay && wasPlaying) {

                    wasPlaying = true;
                    mediaPlayer.pause();
                    //Назначение кнопке картинку плей
                    fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_play));

                }else if(mediaPlayer != null && !isPlay && wasPlaying){
                    mediaPlayer.start();

                    new Thread(this).start(); //Запуск дополнительного потока
                    //Назначение кнопке картинку паузы
                    fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause));

                    wasPlaying = true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMediaPlayer();
    }

    //Уничтожение плеера
    private void clearMediaPlayer() {
        mediaPlayer.stop(); //Останока потока
        mediaPlayer.release(); //Освобожение ресурса
        mediaPlayer = null; //Уничтожает объект
    }

    @Override
    public void run() {
        int currentPosition = mediaPlayer.getCurrentPosition(); //текущая позиция трека
        int total = mediaPlayer.getDuration(); //длина трека

        //бесконечный цикл проигрывания трека пр условии плейер есть и текущая позиция меньше длины трека
        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < total) {

            try {

                Thread.sleep(1000); //засыпание потока на 1 секунду
                currentPosition = mediaPlayer.getCurrentPosition(); //получение позиции трека

            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } catch (Exception e) {
                return;
            }

            seekBar.setProgress(currentPosition); //обновление прогрессбара текущей позицией
        }
    }
}

