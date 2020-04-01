package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dtos.PictureSeedDto;
import softuni.exam.models.entities.Car;
import softuni.exam.models.entities.Picture;
import softuni.exam.repository.PictureRepository;
import softuni.exam.service.CarService;
import softuni.exam.service.PictureService;
import softuni.exam.util.ValidationUtil;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static softuni.exam.constants.GlobalConstants.*;

@Service
public class PictureServiceImpl implements PictureService {
    private final PictureRepository pictureRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final Gson gson;
    private final CarService carService;

    public PictureServiceImpl(PictureRepository pictureRepository, ModelMapper modelMapper, ValidationUtil validationUtil, Gson gson, CarService carService) {
        this.pictureRepository = pictureRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.gson = gson;
        this.carService = carService;
    }

    @Override
    public boolean areImported() {
        return this.pictureRepository.count() > 0;
    }

    @Override
    public String readPicturesFromFile() throws IOException {
        return Files.readString(Path.of(PICTURES_FILE_PATH));
    }

    @Override
    public String importPictures() throws IOException {
        StringBuilder importedResults = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        PictureSeedDto[] pictureSeedDtos =
                this.gson.fromJson(new FileReader(PICTURES_FILE_PATH), PictureSeedDto[].class);

        Arrays.stream(pictureSeedDtos)
                .forEach(pictureSeedDto -> {
                    if (this.validationUtil.isValid(pictureSeedDto)) {
                        if (this.pictureRepository.findByName(pictureSeedDto.getName()) == null) {
                         Picture picture = this.modelMapper.map(pictureSeedDto, Picture.class);
                         LocalDateTime localDateTime = LocalDateTime.parse(pictureSeedDto.getDateAndTime(), dtf);
                         Car car = this.carService.getCarById(pictureSeedDto.getCar());
                         picture.setDateAndTime(localDateTime);
                         picture.setCar(car);
                            this.pictureRepository.saveAndFlush(picture);
                            importedResults.append(
                                    String.format("Successfully import picture - %s",
                                           pictureSeedDto.getName()
                                    ));
                        } else {
                            importedResults.append("Already in DB");
                        }
                    } else {
                        importedResults.append("Invalid picture");
                    }
                    importedResults.append(System.lineSeparator());
                });

        return importedResults.toString();
    }

    @Override
    public int getCountByCarId(Long id) {
        return this.pictureRepository.countAllByCarId(id);
    }
}
