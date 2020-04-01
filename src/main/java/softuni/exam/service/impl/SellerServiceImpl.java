package softuni.exam.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dtos.SellerSeedRootDto;
import softuni.exam.models.entities.Seller;
import softuni.exam.repository.SellerRepository;
import softuni.exam.service.SellerService;
import softuni.exam.util.ValidationUtil;
import softuni.exam.util.XmlParser;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static softuni.exam.constants.GlobalConstants.SELLERS_FILE_PATH;

@Service
public class SellerServiceImpl implements SellerService {
    private final SellerRepository sellerRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final XmlParser xmlParser;

    public SellerServiceImpl(SellerRepository sellerRepository, ModelMapper modelMapper, ValidationUtil validationUtil, XmlParser xmlParser) {
        this.sellerRepository = sellerRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.xmlParser = xmlParser;
    }

    @Override
    public boolean areImported() {
        return this.sellerRepository.count() > 0;
    }

    @Override
    public String readSellersFromFile() throws IOException {
        return Files.readString(Path.of(SELLERS_FILE_PATH));
    }

    @Override
    public String importSellers() throws IOException, JAXBException {
        StringBuilder importResults = new StringBuilder();
        SellerSeedRootDto sellerSeedRootDto =
                this.xmlParser.unmarshalFromFile(SELLERS_FILE_PATH, SellerSeedRootDto.class);

        sellerSeedRootDto.getSellers()
                .forEach(sellerSeedDto -> {
                    if (this.validationUtil.isValid(sellerSeedDto)) {
                        if (this.sellerRepository.findByEmail(sellerSeedDto.getEmail()) == null) {
                            Seller seller = this.modelMapper.map(sellerSeedDto, Seller.class);
                            System.out.println();
                            this.sellerRepository.saveAndFlush(seller);
                            importResults.append(String.format
                                    ("Successfully import seller %s - %s",
                                            sellerSeedDto.getFirstName(),
                                            sellerSeedDto.getEmail()));
                        } else {
                            importResults.append("Already in DB");
                        }
                    } else {
                        importResults.append("Invalid seller");
                    }

                    importResults.append(System.lineSeparator());
                });
        return importResults.toString();
    }

    @Override
    public Seller getSellerById(Long id) {
        return this.sellerRepository.findById(id).orElse(null);
    }
}
