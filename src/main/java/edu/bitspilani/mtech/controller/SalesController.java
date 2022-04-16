/**
 * 
 */
package edu.bitspilani.mtech.controller;


import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.bitspilani.mtech.dto.Error;
import edu.bitspilani.mtech.dto.ProductDTO;
import edu.bitspilani.mtech.model.ResponseModel;
import edu.bitspilani.mtech.model.Sale;
import edu.bitspilani.mtech.repository.SalesRepository;

/**
 * @author Mohamed Noohu
 *
 */
@RestController
@RequestMapping("/api")
public class SalesController {

private static final Logger logger = LoggerFactory.getLogger(SalesController.class);
	
	@Autowired
	private LoadBalancerClient loadBalancerClient;
	
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	SalesRepository repo;
	
	@Autowired
    ObjectMapper objectMapper;
	
	@PostMapping("/sale")
	@Transactional
	public ResponseEntity<?> sales(@RequestPart String productData) throws JsonMappingException, JsonProcessingException {
		ResponseModel rm = new ResponseModel();
		
		logger.info("Entering sale....");
		
		Sale s = this.toSale(productData);
		
		if (s.getCode() == null)
			rm.setError("code","Product code must be specified");
		
		if (s.getQuantity() < 0)
			rm.setError("quantity","Quantity must be greater than 0");
		
		if (s.getPrice() < 0)
			rm.setError("price","Price must be greater than 0");
		
		if (!rm.getErrors().isEmpty())
			return ResponseEntity.ok().body(new Error(rm));
		
		String response = restTemplate.getForObject(getProductServiceBaseUri() + "/api/product/code/"+s.getCode(),String.class);
		logger.info("Get response:"+response);
		
		
		if(response.contains("error"))
			return ResponseEntity.ok().body(response);	

		
		ProductDTO pDto = this.toProductDTO(response);
		
		
		if (s.getPrice() < pDto.getSellingPrice())
			rm.setError("price","Selling price can't be less than product selling price");
		
		if (s.getQuantity() > pDto.getQuantity())
			rm.setError("quantity","Selling quantity can't be more than available quantity");
		
		if (!rm.getErrors().isEmpty())
			return ResponseEntity.ok().body(new Error(rm));
		
		s.setDate(new Date());
		s.setTotal(s.getQuantity() * s.getPrice());
		
		repo.save(s);
		
		pDto.setQuantity(s.getQuantity());
		pDto.setTranType("sale");
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.setAccept(Arrays.asList(MediaType.ALL));
		
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("productData", pDto);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		restTemplate.exchange(getProductServiceBaseUri() + "/api/product", HttpMethod.PUT, requestEntity, String.class);
		
		
		logger.info("update response:"+response);
		
		return ResponseEntity.ok().body(null);
		
	}

	private String getProductServiceBaseUri(){
        ServiceInstance serviceInstance =  loadBalancerClient.choose("product-ms");
        return serviceInstance.getUri().toString();
    }
	
	public Sale toSale(String data) throws JsonMappingException, JsonProcessingException {
		return objectMapper.readValue(data, Sale.class);
	}
	
	public ProductDTO toProductDTO(String data) throws JsonMappingException, JsonProcessingException {
		return objectMapper.readValue(data, ProductDTO.class);
	}
	
	public Error toError(String data) throws JsonMappingException, JsonProcessingException {
		return objectMapper.readValue(data, Error.class);
	}
	
	public String toJsonstring(String data) throws JsonMappingException, JsonProcessingException {
		return objectMapper.writeValueAsString(data);
	}
}
