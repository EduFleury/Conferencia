package src.net.vtec.conferencia.helte;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.parse.ANTLRParser.throwsSpec_return;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sankhya.util.JdbcUtils;
import com.sankhya.util.JsonUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class Conferencia implements AcaoRotinaJava {
	
	public void doAction(ContextoAcao contexto) throws Exception {
		ServiceContext serviceContext = ServiceContext.getCurrent(); // Contexto do Botão de Ação
		
		JapeSession.SessionHandle hnd = null;
		
		//Variaveis
		JdbcWrapper jdbc = null;
		NativeSql sqlCabItem = null;
        NativeSql sql = null;

        ResultSet rsetCabItem = null;
        ResultSet rsetIte = null;;


		
		List<String> codbarraList = new ArrayList<String>(); //Obtem Numreg
		codbarraList = ObtemJson(serviceContext);
		
		ServiceContext sctx = new ServiceContext(null);     // Contexto para Ordem de Produção.
		
		try {
			
			sctx.setAutentication(AuthenticationInfo.getCurrent());
            sctx.makeCurrent();
            
            hnd = JapeSession.open();
            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

            jdbc = dwfFacade.getJdbcWrapper();
            jdbc.openSession();
            
            sqlCabItem = new NativeSql(jdbc);
			sqlCabItem.appendSql(" SELECT * " 
			               + " from TGFBAR "
						   + " WHERE CODBARRA = :P_CODBARRA ");
			
			sqlCabItem.setNamedParameter("P_CODBARRA", codbarraList.get(0));
			
			rsetCabItem = sqlCabItem.executeQuery();
			
			if(rsetCabItem != null) {
				while (rsetCabItem.next()) {
					
					JapeWrapper empresaDAO = JapeFactory.dao("AD_CONFBARVTEC");
					DynamicVO save = empresaDAO.create()
						.set("CODBARRA", rsetCabItem.getString("CODBARRA"))
						.set("CODPROD", rsetCabItem.getBigDecimal("CODPROD"))
						.set("QTDPROD", BigDecimal.valueOf(1))
						.save();
				}
			}else {
				contexto.setMensagemRetorno("Código de Barras inválido");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			serviceContext.makeCurrent();
			
			JdbcUtils.closeResultSet(rsetCabItem);
			JdbcUtils.closeResultSet(rsetIte);
			NativeSql.releaseResources(sql);
			NativeSql.releaseResources(sqlCabItem);			
            JdbcWrapper.closeSession(jdbc);
            JapeSession.close(hnd);
		}
	}
	
	//Funções
		public List<String> ObtemJson(ServiceContext serviceContext) {
			//Request Body 
	        JsonObject requestBody = serviceContext.getJsonRequestBody();

	        // Acessando o objeto "javaCall"
	        JsonObject javaCall = JsonUtils.getJsonObject(requestBody, "javaCall");
	        
	        //a partir de "javaCall" acesso o objeto "rows"
	        JsonObject rows = JsonUtils.getJsonObject(javaCall, "rows");
	        
	        //a partir de "rows" acesso o array "row"
	      	JsonArray row = JsonUtils.getJsonArray(rows, "row");
	      	
	      	//Criando uma lista usando o método List:
	        List<String> codbarraList = new ArrayList<String>();
	      	
	      	for (JsonElement jsonElement : row) {
	      		//a partir do elemento obtenho o array "field"
	            JsonArray fieldArray = JsonUtils.getJsonArray((JsonObject) jsonElement, "field");
	            
	            //obtenho o primeiro elemento do array
	            JsonElement fieldName = fieldArray.get(1);
	            
	            //a partir do elemento obtenho o valor de "$" que é numreg
	            String codbarra = JsonUtils.getString((JsonObject)fieldName, "$");
	            
	            codbarraList.add(codbarra);
	      	}
	      	
	      	return codbarraList;
		}
}
