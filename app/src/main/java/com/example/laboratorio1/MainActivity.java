package com.example.laboratorio1;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity {
    //1)
    Button IdDesconectar, IdEnviar, IdLimpiar, IdEnviar2;
    TextView IdPotenciometro, IdTeclado;
    EditText IdEnviarLcd, IdEnviarLcd2;
    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    // PWM
    private TextView IdNumeroPwm;
    private SeekBar IdSeekBar;
    //grafica
    private LineGraphSeries<DataPoint> series;
    GraphView graph;
    ArrayList listVoltios;

    //-------------------------------------------
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // we get graph view instance
        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setXAxisBoundsManual(true);
        series = new LineGraphSeries<>();
        //
        IdSeekBar = (SeekBar) findViewById(R.id.IdSeekBar);
        // Valor Inicial
        IdSeekBar.setProgress(0);
        // Valot Final
        IdSeekBar.setMax(255);
        IdNumeroPwm = (TextView) findViewById(R.id.IdNumeroPwm);


        //2)
        //Enlaza los controles con sus respectivas vistas
        IdLimpiar = (Button) findViewById(R.id.IdLimpiar);
        IdEnviar = (Button) findViewById(R.id.IdEnviar);
        IdEnviar2 = (Button) findViewById(R.id.IdEnviar2);
        IdPotenciometro = (TextView) findViewById(R.id.IdPotenciometro);
        IdTeclado = (TextView) findViewById(R.id.IdTeclado);
        IdEnviarLcd = (EditText) findViewById(R.id.IdEnviarLcd);
        IdEnviarLcd2 = (EditText) findViewById(R.id.IdEnviarLcd2);
        listVoltios = new ArrayList();
        try {

            bluetoothIn = new Handler() {
                public void handleMessage(android.os.Message msg) {
                    if (msg.what == handlerState) {
                        String readMessage = (String) msg.obj;
                        DataStringIN.append(readMessage);

                        int endOfLineIndex = DataStringIN.indexOf("#");

                        if (endOfLineIndex > 0) {
                            String dataInPrint = DataStringIN.substring(0, endOfLineIndex);

                            if (dataInPrint.contains("aje")) {
                                int index = dataInPrint.indexOf("=");
                                try {
                                    double voltaje = Float.parseFloat(dataInPrint.substring(index + 1));
                                    System.out.println("voltaje: " + voltaje);

                                    //aplicamos patron observer de los patrones de diseño de sftware
                                    System.out.println("voltaje: " + voltaje);
                                    IdPotenciometro.setText("voltaje: " + voltaje);//<-<- PARTE A MODIFICAR >->->
                                    listVoltios.add(voltaje);
                                    chainGraph(listVoltios);
                              /*  if (voltaje <= 5) {
                                    list.add(voltaje);
                                    notificador.graficarVariacionVoltaje(list);
                                }
                                if (voltaje <= 5) {
                                    list.add(voltaje);
                                    notificador.graficarVariacionVoltaje(list);
                                }*/

                                } catch (Exception e) {
                                    System.out.println("exception: " + e.getMessage());
                                }
                            }
                            if (dataInPrint.contains("cla")) {
                                int index = dataInPrint.indexOf("=");
                                char tecla = dataInPrint.charAt(index + 1);
                                System.out.println("tecla: " + tecla);
                                try {
                                    IdTeclado.setText(IdTeclado.getText() + String.valueOf(tecla));
                                } catch (Exception e) {
                                }
                            }
                            DataStringIN.delete(0, DataStringIN.length());
                        }
                    }
                }
            };

        } catch (Exception e) {
            Toast.makeText(this, "Error Inexperado", Toast.LENGTH_SHORT).show();
        }


        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        VerificarEstadoBT();

        // Configuracion onClick listeners para los botones
        // para indicar que se realizara cuando se detecte
        // el evento de Click
        IdEnviar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String cadena = IdEnviarLcd.getText().toString();
                    MyConexionBT.write("L" + IdEnviarLcd.getText().toString() + "\r\n");
                } catch (Exception e) {
                    System.out.println("Error inexperado: " + e.getMessage());
                }
            }
        });

        IdEnviar2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String cadena = IdEnviarLcd2.getText().toString();
                    MyConexionBT.write("D" + cadena + "\r\n");
                } catch (Exception e) {
                    System.out.println("Error inexperado: " + e.getMessage());
                }
            }
        });


        IdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //hace un llamado a la perilla cuando se arrastra
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    IdNumeroPwm.setText(String.valueOf(progress));
                    MyConexionBT.write("P" + IdNumeroPwm.getText().toString() + "\r\n");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            //hace un llamado  cuando se toca la perilla
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            //hace un llamado  cuando se detiene la perilla
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        IdLimpiar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MyConexionBT.write("L.\r\n");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                desconectar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void desconectar() {
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();

            }
        }
        finish();
    }


    private void chainGraph(ArrayList list) {
        graph.removeAllSeries();
        int count = list.size();
        DataPoint[] values = new DataPoint[count];
        for (int i = 0; i < count; i++) {
            double x = i;
            double y = Double.parseDouble(list.get(i).toString());
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }

        graph.getViewport().setMaxX(count);
        series = new LineGraphSeries<>(values);
        graph.addSeries(series);
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Consigue la direccion MAC desde DeviceListActivity via intent
        Intent intent = getIntent();
        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);//<-<- PARTE A MODIFICAR >->->
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try {
            btSocket.connect();
        } catch (IOException e) {
            desconectar();
            Toast.makeText(this, "Error al conectar", Toast.LENGTH_LONG).show();
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        try { // Cuando se sale de la aplicación esta parte permite
            // que no se deje abierto el socket
            btSocket.close();
        } catch (IOException e2) {
        }
    }

    //Comprueba que el dispositivo Bluetooth Bluetooth está disponible y solicita que se active si está desactivado
    private void VerificarEstadoBT() {

        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Envio de trama
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

}
