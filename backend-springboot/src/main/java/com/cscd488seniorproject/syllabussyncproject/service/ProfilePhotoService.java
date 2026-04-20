package com.cscd488seniorproject.syllabussyncproject.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.cscd488seniorproject.syllabussyncproject.entity.UserAccountEntity;
import com.cscd488seniorproject.syllabussyncproject.repository.UserAccountRepository;

/*
* This service class is responsible for handling profile photo uploads and retrievals.
* It includes methods to upload a new profile photo for the authenticated user, 
* and to retrieve the current profile photo. Uploaded photos are processed to be square
* and resized to a standard avatar size, then stored in the database as JPEG with a timestamp
* of when it was last updated.
*/
@Service
public class ProfilePhotoService {

    private static final long MAX_BYTES = 2L * 1024L * 1024L; // 2MB limit for photos
    private static final int AVATAR_SIZE = 256; // final avatar size will be 256x256 pixels
    private static final float JPEG_QUALITY = 0.85f; // quality for JPEG compression (0.0 to 1.0)

    // Repository for accessing user accounts in the database
    private final UserAccountRepository userRepo;

    //constructor injection of the UserAccountRepository
    public ProfilePhotoService(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Uploads a new profile photo for the authenticated user.
     * The photo is processed to be square and resized to a standard avatar size,
     * then stored in the database as JPEG with a timestamp of when it was last updated.
     */

    @Transactional
    public void uploadMyPhoto(Authentication auth, MultipartFile photo) {
        UserAccountEntity user = requireUser(auth);

        // just some basic validation

        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No photo uploaded");
        }
        if (photo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo too large (max 2MB)");
        }

        // validate content type (only allow JPEG and PNG)
        String contentType = photo.getContentType() == null ? "" : photo.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type (use JPG or PNG)");
        }

        //BufferImage is used to read image files and process it for cropping and resizing
        BufferedImage decoded;
        try (InputStream in = photo.getInputStream()) {
            decoded = ImageIO.read(in);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image");
        }
        if (decoded == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image");
        }

        // Process the image: center crop to square, then resize to avatar size
        BufferedImage square = centerCropSquare(decoded);
        BufferedImage resized = resize(square, AVATAR_SIZE, AVATAR_SIZE);

        // stores image as JPEG to save space
        byte[] jpegBytes = encodeJpeg(resized, JPEG_QUALITY);

        // Update the user's profile photo in the database with the new JPEG bytes, content type, and current timestamp
        user.setProfilePhoto(jpegBytes);
        user.setProfilePhotoContentType("image/jpeg");
        user.setProfilePhotoUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
    }

    /**
     * Retrieves the profile photo for the authenticated user.
     * Returns a PhotoPayload containing the photo bytes, content type, and last updated timestamp.
     * This will be used by the frontend to display user's profile photo in the UI
     */
    @Transactional(readOnly = true)
    public PhotoPayload getMyPhoto(Authentication auth) {
        UserAccountEntity user = requireUser(auth);

        byte[] bytes = user.getProfilePhoto();
        String ct = user.getProfilePhotoContentType();
        LocalDateTime updatedAt = user.getProfilePhotoUpdatedAt();

        if (bytes == null || bytes.length == 0 || ct == null || ct.isBlank()) {
            return null; // caller can translate to 204 No Content
        }
        return new PhotoPayload(bytes, ct, updatedAt);
    }

    // This record class is used to represent the payload of a profile photo, including the photo bytes, content type, and last updated timestamp.
    public record PhotoPayload(byte[] bytes, String contentType, LocalDateTime updatedAt) {}

    /*
     * Helper Method ensures the user is authenticated and retrieves the corresponding UserAccountEntity.
     * Throws a ResponseStatusException with HttpStatus.UNAUTHORIZED if the user is not authenticated.
     */
    private UserAccountEntity requireUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String email = auth.getName() == null ? "" : auth.getName().trim().toLowerCase(Locale.ROOT);
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    /*
     * Helper Method to center crop an image to a square.
     * Takes a BufferedImage as input and returns a square BufferedImage.
     */
    private static BufferedImage centerCropSquare(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2;
        int y = (h - side) / 2;
        return src.getSubimage(x, y, side, side);
    }

    /*
     * Helper Method to resize an image to the specified width and height.
     * Takes a BufferedImage, target width, and target height as input and returns a resized BufferedImage.
     */
    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /*
     * Helper Method to encode a BufferedImage as a JPEG byte array with the specified quality.
     * Takes a BufferedImage and a quality value (0.0 to 1.0) as input and returns a byte array.
     */
    private static byte[] encodeJpeg(BufferedImage img, float quality) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No JPEG writer available");
            }
            ImageWriter writer = writers.next();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);

                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(Math.max(0f, Math.min(1f, quality)));
                }

                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }

            return baos.toByteArray();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to encode JPEG");
        }
    }
}