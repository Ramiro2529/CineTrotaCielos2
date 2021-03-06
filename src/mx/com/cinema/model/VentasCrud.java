package mx.com.cinema.model;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mx.com.cinema.entities.Boleto;
import mx.com.cinema.entities.UsuarioBean;
import mx.com.cinema.entities.VentaBoletosBean;


public class VentasCrud {

	Connection con;
	ConnectionDB conexion;
	CallableStatement cmt;
	ResultSet rs;
	 
	
	/*parametros a recibir en servelet 
	 * 
	 * Bean 
	 * 
	 * idFuncion 
	 * numeroDeAsientos 
	 * dia de la semana 0 1 2 3 4 5 6  
	 * 
	 * 
	 * */

	public VentaBoletosBean getInfoVenta(VentaBoletosBean parametrosVenta){
		
		String textoProcedure = "{call  P_FORMATO_PRECIO( ? )}";
		String procPromocion = "{call P_PROMO_DIA( ? )}";
		float subtotal = 1;
		float total = 1;
		String descuentoo;
		conexion = new ConnectionDB();
		con = conexion.getConexion();
		try {
			cmt = con.prepareCall(textoProcedure);
			cmt.setInt(1, parametrosVenta.getIdFuncion());
			rs = cmt.executeQuery();
			while(rs.next()) {
				parametrosVenta.setTipoBoleto(rs.getString("Formato"));
				subtotal = rs.getFloat("Precio");
			}
			subtotal = subtotal * parametrosVenta.getNumeroAsientos();
			parametrosVenta.setSubtotal(subtotal);
			cmt = con.prepareCall(procPromocion);
			cmt.setInt(1, parametrosVenta.getDia());
			rs = cmt.executeQuery();

			if(rs.next()) {
				parametrosVenta.setNombreDescuento(rs.getString("Nombre"));
				total = rs.getFloat("Descuento");
				descuentoo = Math.round(total*100) + "%"; 
				parametrosVenta.setDescuento(descuentoo);
				total = subtotal - ( total * subtotal);
				parametrosVenta.setTotal(total);
			}			
			else {
				total = subtotal;
				parametrosVenta.setTotal(total);
			}
			//infoVenta.setNumeroAsientos(parametrosVenta.getNumeroAsientos());
			con.close();
		}catch(SQLException sqle) {
			System.out.println(sqle.getMessage());
		}
		System.out.println(parametrosVenta);
		return parametrosVenta;
	}
	
	
	public int validarAsientos(int [] idAsientos,int idFuncion) {
		// validamos que los asientos seleccionados sigan disponibles
		conexion = new ConnectionDB();
		con = conexion.getConexion();
		String checarDispo = "{call P_DISPO_ASIENTO(?, ?)}";
		int asientoOcupado = 0; //0 que esta disponible, 1 ya no esta disponible
		
		try {
			for(int idAsiento: idAsientos) {
				cmt = con.prepareCall(checarDispo);
				cmt.setInt(1,idAsiento);
				cmt.setInt(2,idFuncion);
				rs = cmt.executeQuery();
				if(rs.next()) {
					asientoOcupado = rs.getInt("disponible");
					if(asientoOcupado > 0) {
						return asientoOcupado;
					}
				}
			}
		}catch(SQLException sqle) {
			System.out.println(sqle.getMessage());
		}finally {
			try {
				if(rs != null) {
					rs.close();
				}
			}catch(SQLException sqle) {
				System.out.println(sqle.getMessage());
			}
			try {
				if(cmt !=null) {
					cmt.close();
				}
			}catch(SQLException sqle) {
				System.out.println(sqle.getMessage());
			}
			try {
				if(con != null) {
					con.close();
				}
			}catch(SQLException sqle) {
				System.out.println(sqle.getMessage());
			}
		}
		return asientoOcupado;
	}
	
	public int generarTicket(VentaBoletosBean parametrosVenta,UsuarioBean usuarioLogin){		
		int respuesta = 0;
		String obtenerTicket ="{call P_VENTA_TICKET(?, ? , ?)}";
		String insertarAsientos="{call P_VENTA_ASIENTO(? , ?, ?)}";
		conexion = new ConnectionDB();
		con = conexion.getConexion();
		try {
			cmt = con.prepareCall(obtenerTicket);
			cmt.setInt(1,parametrosVenta.getIdFuncion());
			cmt.setDouble(2, usuarioLogin.getIdTarjeta());
			cmt.setFloat(3, parametrosVenta.getTotal());
			rs = cmt.executeQuery();
			if(rs.next()) {
				parametrosVenta.setTicket(rs.getInt("idVenta"));
				for(int x: parametrosVenta.getArregloAsientos()) {
					cmt = con.prepareCall(insertarAsientos);
					cmt.setInt(1, parametrosVenta.getTicket());
					cmt.setInt(2, x);
					cmt.setInt(3, parametrosVenta.getIdFuncion());
					rs = cmt.executeQuery();
					if(rs.next()) {
						 respuesta = rs.getInt("Status");
					}else {
						respuesta = -1;
					}	
				}
			}else {
				respuesta = -1;
			}
			con.close();
		}catch(SQLException sql) {
			System.out.println("Es problema con la BD" + sql.getMessage());
		}finally { // es lo que va a ejecutar despu�s de que haya termianado con el try catch independientemenete que haya una excepci�n
			System.out.println("Me ejecute al final");
		}
		return respuesta;
	}
	
	public List<Boleto> getInfoBoleto(int idFuncion, int [] idAsientos) {
		String procInfoBoletos = "{ call P_info_boleto(?,?)}";
		List<Boleto> boletos = new ArrayList<Boleto>();
		conexion = new ConnectionDB();
		con = conexion.getConexion();
		try {
			for(int idAsiento: idAsientos) {
				cmt = con.prepareCall(procInfoBoletos);
				cmt.setInt(1, idFuncion);
				cmt.setInt(2, idAsiento);
				rs = cmt.executeQuery();
				if(rs.next()) {
					Boleto infoBoleto = new Boleto();
					infoBoleto.setSala(rs.getString("NombreSala"));
					infoBoleto.setPelicula(rs.getString("pelicula"));
					infoBoleto.setNombre(rs.getString("nombreasiento"));
					infoBoleto.setHora(rs.getString("hora"));
					infoBoleto.setFecha(rs.getString("dia"));
					boletos.add(infoBoleto);
				}
			}
		}catch(SQLException sqle) {
			System.out.println(sqle.getMessage());
		}
		return boletos;
	}
}
