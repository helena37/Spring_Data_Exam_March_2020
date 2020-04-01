package softuni.exam.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import softuni.exam.models.dtos.OfferSeedRootDto;
import softuni.exam.models.entities.Car;
import softuni.exam.models.entities.Offer;
import softuni.exam.models.entities.Seller;
import softuni.exam.repository.OfferRepository;
import softuni.exam.service.CarService;
import softuni.exam.service.OfferService;
import softuni.exam.service.SellerService;
import softuni.exam.util.ValidationUtil;
import softuni.exam.util.XmlParser;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static softuni.exam.constants.GlobalConstants.*;

@Service
public class OfferServiceImpl implements OfferService {
    private final OfferRepository offerRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final XmlParser xmlParser;
    private final CarService carService;
    private final SellerService sellerService;

    @Autowired
    public OfferServiceImpl(OfferRepository offerRepository, ModelMapper modelMapper, ValidationUtil validationUtil, XmlParser xmlParser, CarService carService, SellerService sellerService) {
        this.offerRepository = offerRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.xmlParser = xmlParser;
        this.carService = carService;
        this.sellerService = sellerService;
    }

    @Override
    public boolean areImported() {
        return this.offerRepository.count() > 0;
    }

    @Override
    public String readOffersFileContent() throws IOException {
        return Files.readString(Path.of(OFFERS_FILE_PATH));
    }

    @Override
    public String importOffers() throws IOException, JAXBException {
        StringBuilder importResults = new StringBuilder();
        OfferSeedRootDto offerSeedRootDto =
                this.xmlParser.unmarshalFromFile(OFFERS_FILE_PATH, OfferSeedRootDto.class);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        offerSeedRootDto.getOffers()
                .forEach(offerSeedDto -> {
                    LocalDateTime localDateTime = LocalDateTime.parse(offerSeedDto.getAddedOn(), dtf);
                    if (this.validationUtil.isValid(offerSeedDto)) {
                        if (this.offerRepository.findByDescriptionAndAddedOn(
                                offerSeedDto.getDescription(), localDateTime) == null) {
                            Offer offer = this.modelMapper.map(offerSeedDto, Offer.class);
                            offer.setAddedOn(localDateTime);
                            Car car = this.carService.getCarById(offerSeedDto.getCar().getId());
                            Seller seller = this.sellerService.getSellerById(offerSeedDto.getSeller().getId());
                            offer.setCar(car);
                            offer.setSeller(seller);
                            this.offerRepository.saveAndFlush(offer);
                            importResults.append(String.format(
                                    "Successfully import offer %s - %s",
                                    offerSeedDto.getAddedOn(),
                                    offerSeedDto.getHasGoldStatus()
                            ));
                        } else {
                            importResults.append("Already in DB");
                        }
                    } else {
                        importResults.append("Invalid offer");
                    }

                    importResults.append(System.lineSeparator());
                });
        return importResults.toString();
    }
}
