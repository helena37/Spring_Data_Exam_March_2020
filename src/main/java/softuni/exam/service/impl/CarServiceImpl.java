package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dtos.CarSeedDto;
import softuni.exam.models.entities.Car;
import softuni.exam.repository.CarRepository;
import softuni.exam.repository.PictureRepository;
import softuni.exam.service.CarService;
import softuni.exam.util.ValidationUtil;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static softuni.exam.constants.GlobalConstants.CARS_FILE_PATH;

@Service
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final Gson gson;
    private final PictureRepository pictureRepository;

    public CarServiceImpl(CarRepository carRepository, ModelMapper modelMapper, ValidationUtil validationUtil, Gson gson, PictureRepository pictureRepository) {
        this.carRepository = carRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.gson = gson;
        this.pictureRepository = pictureRepository;
    }


    @Override
    public boolean areImported() {

        return this.carRepository.count() > 0;
    }

    @Override
    public String readCarsFileContent() throws IOException {
        return Files.readString(Path.of(CARS_FILE_PATH));
    }

    @Override
    public String importCars() throws IOException {
        StringBuilder importedResults = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        CarSeedDto[] carSeedDtos =
                this.gson.fromJson(new FileReader(CARS_FILE_PATH), CarSeedDto[].class);

        Arrays.stream(carSeedDtos)
                .forEach(carSeedDto -> {
                    if (this.validationUtil.isValid(carSeedDto)) {
                        if (this.carRepository.findCarByMakeAndModelAndKilometers(
                                carSeedDto.getMake(), carSeedDto.getModel(), carSeedDto.getKilometers()) == null) {
                            LocalDate localDate = LocalDate.parse(carSeedDto.getRegisteredOn(), dtf);
                            Car car = this.modelMapper.map(carSeedDto, Car.class);
                            car.setRegisteredOn(localDate);
                            this.carRepository.saveAndFlush(car);
                            importedResults.append(
                                    String.format("Successfully imported car - %s - %s",
                                            carSeedDto.getMake(),
                                            carSeedDto.getModel()
                                            ));
                        } else {
                           importedResults.append("Already in DB");
                        }
                    } else {
                        importedResults.append("Invalid car");
                    }
                    importedResults.append(System.lineSeparator());
                });

        return importedResults.toString();
    }

    @Override
    public String getCarsOrderByPicturesCountThenByMake() {
        StringBuilder exportResults = new StringBuilder();
        List<Car> cars = this.carRepository.findAll();
        cars.forEach(car -> exportResults.append(String.format(
                    "Car make - %s, model - %s\n" +
                            "\tKilometers - %d\n" +
                            "\tRegistered on - %s\n" +
                            "\tNumber of pictures - %d\n",
                    car.getMake(),
                    car.getModel(),
                    car.getKilometers(),
                    car.getRegisteredOn(),
                    this.pictureRepository.countAllByCarId(car.getId())
            )));
        return exportResults.toString();
    }

    @Override
    public Car getCarById(Long id) {
        return this.carRepository.findById(id).orElse(null);
    }


}
