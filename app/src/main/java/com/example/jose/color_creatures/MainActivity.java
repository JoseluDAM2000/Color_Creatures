package com.example.jose.color_creatures;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Button boton;
    private TextView texto;
    private TextView tvcolores;
    private ImageView ivMonigote;
    private ImageView ivCarne;
    private String colorFoto;
    private String colorActual;

    private String currentPhotoPath;

    private final String AZUL = "AZUL";
    private final String ROJO = "ROJO";
    private final String VERDE = "VERDE";
    private final String GRIS = "GRIS";

    private final String CARPETA_IMAGEN = "ColorCreatures/";
    private final String RUTA_IMAGEN = CARPETA_IMAGEN + "ColorCreatures";

    private final int REQUEST_IMAGE_CAPTURE = 1;
    private final int REQUEST_STORAGE = 2;
    private final int REQUEST_READ_STORAGE = 3;
    private final int REQUEST_GALERY = 4;

    private final int TOLERANCIA_GRISES = 40;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pedirPermisos();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //No es una buena práctica hacer lecturas/escrituras desde el hilo principal,
        //pero como me dio problemas el otro dia haciendolo desde un hilo tengo que
        //machacar la politica de la app para poder hacerlo desde aquí.
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());


        tvcolores = findViewById(R.id.textViewColores);
        boton = findViewById(R.id.botonCamara);
        texto = findViewById(R.id.texto);
        ivMonigote = findViewById(R.id.monigote);
        ivCarne = findViewById(R.id.carne);

        ivCarne.setBackgroundResource(R.drawable.animacion_carne);
        texto.setText(getString(R.string.texto_defecto));
        boton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (boton.getText().equals(getString(R.string.texto_boton_siguiente))) {
                    cambiarMonigote();
                    boton.setText(getString(R.string.texto_boton_foto));
                } else {
                    cargarImagen();
                }
            }
        });
        cambiarMonigote();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bitmap imagen = null;
            switch (requestCode) {
                case REQUEST_GALERY:
                    try {
                        imagen = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                    } catch (IOException e) {
                        Toast.makeText(this, getString(R.string.error_lectura), Toast.LENGTH_LONG).show();
                    }
                    break;
                case REQUEST_IMAGE_CAPTURE:
                    MediaScannerConnection.scanFile(this, new String[]{currentPhotoPath}, null, new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String s, Uri uri) {
                        }
                    });
                    imagen = BitmapFactory.decodeFile(currentPhotoPath);
                    break;
            }
            colorFoto = obtenerColorDominante(imagen);
            chequearFoto();
        }
    }

    private void cargarImagen() {
        final CharSequence[] opciones = getResources().getStringArray(R.array.opciones);
        final AlertDialog.Builder alertaOpciones = new AlertDialog.Builder(MainActivity.this);
        alertaOpciones.setTitle(getString(R.string.titulo_menu));

        alertaOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals(opciones[0])) {
                    cargarImagenDesdeLaCamara();
                } else {
                    if (opciones[i].equals(opciones[1])) {
                        cargarImagenDesdeLaGaleria();
                    } else {
                        dialogInterface.dismiss();
                    }
                }
            }
        });
        alertaOpciones.show();
    }

    private void cargarImagenDesdeLaCamara() {
        //Creamos la ruta donde almacenaremos las imagenes.
        File fileImage = new File(Environment.getExternalStorageDirectory(), RUTA_IMAGEN);

        //Comprobamos si el fichero existe y de no ser así se crea.
        boolean isCreada = fileImage.exists();

        if (!isCreada) {
            isCreada = fileImage.mkdirs();
        }

        String nombreImagen = "";
        if (isCreada) {
            //Usamos un truco simple para que no haya imagenes con el mismo nombre y le concatenamos la extensión.
            nombreImagen = (System.currentTimeMillis() / 1000) + ".png";
        }

        //Guardamos la ruta en la que escribimos las imagenes y creamos un File apuntando a la misma.
        currentPhotoPath = Environment.getExternalStorageDirectory() + File.separator + RUTA_IMAGEN + File.separator + nombreImagen;

        File imagen = new File(currentPhotoPath);

        //Creamos el Intent para llamar a la Camara.
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imagen));
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void cargarImagenDesdeLaGaleria() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType(getString(R.string.tipo_imagen));
        startActivityForResult(intent.createChooser(intent, getString(R.string.titulo_chooser)), REQUEST_GALERY);
    }

    private void pedirPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            }
        }
    }

    private String obtenerColorDominante(Bitmap mapaDeBits) {
        if (null == mapaDeBits) return null;

        int cuentaRojo = 0;
        int cuentaVerde = 0;
        int cuentaAzul = 0;

        int cuentaPixeles = mapaDeBits.getWidth() * mapaDeBits.getHeight();
        int[] pixels = new int[cuentaPixeles];

        mapaDeBits.getPixels(pixels, 0, mapaDeBits.getWidth(), 0, 0, mapaDeBits.getWidth(), mapaDeBits.getHeight());

        for (int y = 0, h = mapaDeBits.getHeight(); y < h; y++) {
            for (int x = 0, w = mapaDeBits.getWidth(); x < w; x++) {
                int color = pixels[x + y * w];
                cuentaRojo += (color >> 16) & 0xFF;
                cuentaVerde += (color >> 8) & 0xFF;
                cuentaAzul += (color & 0xFF);
            }
        }

        cuentaRojo /= cuentaPixeles;
        cuentaVerde /= cuentaPixeles;
        cuentaAzul /= cuentaPixeles;

        tvcolores.setText("Rojo: " + cuentaRojo + "\nVerde: " + cuentaVerde + "\nAzul: " + cuentaAzul);

        String valorDeRetorno;

        if (posibleGris(cuentaRojo, cuentaVerde, cuentaAzul)) {
            tvcolores.setTextColor(Color.RED);
            return GRIS;
        }

        if (cuentaRojo > cuentaVerde && cuentaRojo > cuentaAzul) {
            valorDeRetorno = ROJO;
        } else if (cuentaVerde > cuentaAzul) {
            valorDeRetorno = VERDE;
        } else {
            valorDeRetorno = AZUL;
        }

        return valorDeRetorno;
    }

    private void cambiarMonigote() {
        Random rnd = new Random();
        int numeroRandom = rnd.nextInt(30);
        texto.setText(getString(R.string.texto_defecto));
        switch (numeroRandom) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                ivMonigote.setBackgroundResource(R.drawable.animacion_rojo_zombie);
                colorActual = ROJO;
                break;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                ivMonigote.setBackgroundResource(R.drawable.animacion_rojo_mosca);
                colorActual = ROJO;
                break;
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
                ivMonigote.setBackgroundResource(R.drawable.animacion_verde_pajaro);
                colorActual = VERDE;
                break;
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
                ivMonigote.setBackgroundResource(R.drawable.animacion_verde_ciclope);
                colorActual = VERDE;
                break;
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
                colorActual = AZUL;
                ivMonigote.setBackgroundResource(R.drawable.animacion_azul_murcielago);
                break;
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
                colorActual = AZUL;
                ivMonigote.setBackgroundResource(R.drawable.animacion_azul_gusano);
                break;
        }
        // Get the background, which has been compiled to an AnimationDrawable object.
        AnimationDrawable frameAnimation = (AnimationDrawable) ivMonigote.getBackground();

        // Start the animation (looped playback by default).
        frameAnimation.start();

    }

    private void chequearFoto() {
        if (colorFoto.equals(colorActual)) {
            texto.setText(getString(R.string.texto_bien));

            ivCarne.setVisibility(View.VISIBLE);
            AnimationDrawable animacionCarne = (AnimationDrawable) ivCarne.getBackground();
            animacionCarne.start();

            boton.setText(getString(R.string.texto_boton_siguiente));

            tvcolores.setTextColor(Color.WHITE);

        } else {
            texto.setText(getString(R.string.texto_mal));
        }
    }

    private boolean posibleGris(int r, int g, int b) {
        int rMin = r - TOLERANCIA_GRISES;
        int rMax = r + TOLERANCIA_GRISES;
        int gMin = g - TOLERANCIA_GRISES;
        int gMax = g + TOLERANCIA_GRISES;
        int bMin = b - TOLERANCIA_GRISES;
        int bMax = b + TOLERANCIA_GRISES;
        if ((r < gMax && r > gMin && r < bMax && r > bMin) || (g < rMax && g > rMin && g < bMax && g > bMin) || (b < rMax && b > rMin && b < gMax && b > gMin)) {
            return true;
        }
        return false;
    }
}