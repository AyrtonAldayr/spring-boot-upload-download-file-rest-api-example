package com.hendisantika.uploaddownload.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hendisantika.uploaddownload.payload.UploadFileResponse;
import com.hendisantika.uploaddownload.service.FileStorageService;

@RestController
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	@Autowired
	private FileStorageService fileStorageService;

	@PostMapping("/uploadFile")
	public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) throws IOException {

		//lecturaArchivo(file);
		
		String fileName = fileStorageService.storeFile(file);

		String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/downloadFile/")
				.path(fileName).toUriString();

		return new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
	}

	@PostMapping("/uploadMultipleFiles")
	public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {

		return Arrays.asList(files).stream().map(file -> {
			try {
				return uploadFile(file);
			} catch (IOException e) {				
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toList());

	}

	@GetMapping("/downloadFile/{fileName:.+}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
		// Load file as Resource
		Resource resource = fileStorageService.loadFileAsResource(fileName);

		// Try to determine file's content type
		String contentType = null;
		try {
			contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			logger.info("Could not determine file type.");
		}

		// Fallback to the default content type if type could not be determined
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	public void lecturaArchivo(MultipartFile multipartFile) throws IOException {
		InputStream file = multipartFile.getInputStream();
		Workbook workbook = WorkbookFactory.create(file);
		
		// Recuperando el número de hojas en el Libro de trabajo
		System.out.println("Workbook has " + workbook.getNumberOfSheets() + " Sheets : ");
		
		/**
        *=============================================================
        *Iterando sobre todas las hojas del libro de trabajo (Múltiples formas)
        *=============================================================
		**/
		// 1. Puedes obtener un SheetIterator e iterar sobre él
		Iterator<Sheet> sheetIterator = workbook.sheetIterator();
		System.out.println("Recuperando Hojas usando Iterator");
        while (sheetIterator.hasNext()) {
            Sheet sheet = sheetIterator.next();
            System.out.println("=> " + sheet.getSheetName());
        }
        
        // 2. O puede usar un bucle para cada
        System.out.println("Recuperando hojas usando para cada ciclo");
        for(Sheet sheet: workbook) {
            System.out.println("=> " + sheet.getSheetName());
        }
        
        // 3. O puede usar un Java 8 para cada uno con lambda
        System.out.println("Recuperando hojas usando Java 8 para cada uno con lambda");
        workbook.forEach(sheet -> {
            System.out.println("=> " + sheet.getSheetName());
        });
        
        /**
        ==================================================================
        Iterating over all the rows and columns in a Sheet (Multiple ways)
        ==================================================================
        **/
        
        // Getting the Sheet at index zero
        Sheet sheet = workbook.getSheetAt(0);

        // Create a DataFormatter to format and get each cell's value as String
        DataFormatter dataFormatter = new DataFormatter();
        
        // 1. Puede obtener un rowIterator y columnIterator e iterar sobre ellos
        System.out.println("\n\nIterando sobre Filas y Columnas usando Iterator\n");
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            // Ahora iteremos sobre las columnas de la fila actual
            Iterator<Cell> cellIterator = row.cellIterator();

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String cellValue = dataFormatter.formatCellValue(cell);
                System.out.print(cellValue + "\t");
            }
            System.out.println();
        }
        
        // 2. O puede usar un ciclo for-each para iterar sobre las filas y columnas
        System.out.println("\n\nIterando sobre filas y columnas usando para cada ciclo\n");
        for (Row row: sheet) {
            for(Cell cell: row) {
                String cellValue = dataFormatter.formatCellValue(cell);
                System.out.print(cellValue + "\t");
            }
            System.out.println();
        }
        
        // 3. O puede usar Java 8 para cada ciclo con lambda
        System.out.println("\n\nIterando sobre filas y columnas usando Java 8 para cada uno con lambda\n");
        sheet.forEach(row -> {
            row.forEach(cell -> {
                String cellValue = dataFormatter.formatCellValue(cell);
                System.out.print(cellValue + "\t");
            });
            System.out.println();
        });
        
        // Cerrar el workbook
        workbook.close();
	}

}