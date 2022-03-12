package com.github.assemblathe1.chartographer;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.repositories.PicturesRepository;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PicturesControllerStatusOkTests {
    @Autowired
    private MockMvc mvc;
    Picture picture = new Picture();
    long pictureByteSize;

    @PostConstruct
    public void postConstruct() {
        picture.setId(1L);
        picture.setWidth(51);
        picture.setHeight(102);
        pictureByteSize = 54 + picture.getWidth() * picture.getHeight() * 3L + picture.getHeight() * (picture.getWidth() * 3 % 4 == 0 ? 0 : 4 - (picture.getWidth() * 3 % 4));
    }

    @MockBean
    private PicturesRepository picturesRepository;

    private final String tmpdir = System.getProperty("java.io.tmpdir");

    private String getTestFile(String fileName) {
        return getClass().getClassLoader().getResource("pictures/" + fileName).getPath();
    }

    @Test
    public void givenPicture_whenSaveNewPicture_thenStatus201andIDReturns() throws Exception {
        String createdPicture = tmpdir + "whenSaveNewPicture.bmp";
        Files.deleteIfExists(new File(createdPicture).toPath());
        assertThat(new File(createdPicture)).doesNotExist();
        picture.setUrl(createdPicture);
        given(picturesRepository.save(Mockito.any())).willReturn(picture);
        mvc
                .perform(post("/chartas/{width}&{height}", picture.getWidth(), picture.getHeight()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().string(picture.getId().toString()));
        assertThat(new File(createdPicture)).exists();
    }

    @Test
    public void givenId_whenSaveMultipartPictureFragment_thenStatus200() throws Exception {
        File source = new File(getTestFile("whenSaveMultipartPicture.bmp"));
        File copied = new File(tmpdir + "whenSaveMultipartPicture.bmp");
        picture.setUrl(copied.getAbsolutePath());
        Files.deleteIfExists(copied.toPath());
        assertThat(copied).doesNotExist();
        assertThat(source).exists().hasSize(pictureByteSize);
        FileUtils.copyFile(source, copied);
        assertThat(copied).exists();
        assertThat(Files.readAllLines(source.toPath()).equals(Files.readAllLines(copied.toPath())));
        FileInputStream fileInputStream = new FileInputStream(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        MockMultipartFile pictureFragment = new MockMultipartFile("file", "whenSaveMultipartPictureFragment.bmp",
                String.valueOf(MediaType.valueOf("image/bmp")), fileInputStream);

        int restoringPicturePartWidth = 31;
        int restoringPicturePartHeight = 26;

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        mvc
                .perform(multipart("/chartas/{id}/", picture.getId())
                        .file(pictureFragment)
                        .param("x", String.valueOf(0))
                        .param("y", String.valueOf(0))
                        .param("width", String.valueOf(restoringPicturePartWidth))
                        .param("height", String.valueOf(restoringPicturePartHeight)
                        )
                ).andDo(print())
                .andExpect(status().isOk());
        assertThat(copied).exists();
    }

    @Test
    public void givenId_whenGetMultipartPictureFragment_thenStatus200andMultipartPictureFragmentReturns() throws Exception {
        String sourcePicture = getTestFile("whenGetMultipartPictureFragment.bmp");
        picture.setUrl(sourcePicture);
        assertThat(new File(sourcePicture)).exists().hasSize(pictureByteSize);

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        int returningPictureWidth = 31;
        int returningPictureHeight = 26;
        long returningPictureByteSize = 54 + returningPictureWidth * returningPictureHeight * 3L + returningPictureHeight * (returningPictureWidth * 3 % 4 == 0 ? 0 : 4 - (returningPictureWidth * 3 % 4));

        mvc
                .perform(get("/chartas/{id}/", picture.getId())
                        .param("x", String.valueOf(0))
                        .param("y", String.valueOf(0))
                        .param("width", String.valueOf(returningPictureWidth))
                        .param("height", String.valueOf(returningPictureHeight)
                        )
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("image/bmp")))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(returningPictureByteSize)));
    }

    @Test
    public void givenId_whenDeletePicture_thenStatus200() throws Exception {
        File source = new File(getTestFile("whenDeletePicture.bmp"));
        File copied = new File(tmpdir + "whenDeletePicture.bmp");
        picture.setUrl(copied.getAbsolutePath());
        Files.deleteIfExists(copied.toPath());
        assertThat(copied).doesNotExist();
        assertThat(source).exists();
        FileUtils.copyFile(source, copied);
        assertThat(copied).exists();
        assertThat(Files.readAllLines(source.toPath()).equals(Files.readAllLines(copied.toPath())));

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        mvc
                .perform(delete("/chartas/{id}/", picture.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(copied).doesNotExist();
    }
}